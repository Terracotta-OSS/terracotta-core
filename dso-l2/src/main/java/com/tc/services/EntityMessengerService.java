/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.services;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ManagedEntity.LifecycleListener;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.util.Assert;
import java.util.HashSet;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.terracotta.entity.CommonServerEntity;

/**
 * Implements the IEntityMessenger interface by maintaining a "fake" EntityDescriptor (as there is no actual reference from
 * a client) and using that to send "fake" VoltronEntityMessage instances into the server's message sink.
 */
public class EntityMessengerService implements IEntityMessenger, LifecycleListener {
  private final ISimpleTimer timer;
  private final Sink<VoltronEntityMessage> messageSink;
  private final ManagedEntity owningEntity;
  private final boolean waitForReceived;
  private final RetirementManager retirementManager;
  private final MessageCodec<EntityMessage, ?> codec;
  private final EntityDescriptor fakeDescriptor;
  private final Set<TokenID> liveTokens = new HashSet<>();
  private final ConcurrentHashMap<ExplicitRetirementHandle, Handle> retirementHandles = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public EntityMessengerService(ISimpleTimer timer,
                                Sink<VoltronEntityMessage> messageSink,
                                ManagedEntity owningEntity, boolean waitForReceived) {
    Assert.assertNotNull(timer);
    Assert.assertNotNull(messageSink);
    Assert.assertNotNull(owningEntity);

    this.timer = timer;
    this.messageSink = messageSink;
    this.owningEntity = owningEntity;
    this.waitForReceived = waitForReceived;
    // We need access to the retirement manager in order to build dependencies between messages on this entity.
    this.retirementManager = owningEntity.getRetirementManager();
    // If this service is being created, we expect that the entity has a retirement mananger.
    Assert.assertTrue(null != this.retirementManager);
    // Note that the codec will actually expect to work on a sub-type of EntityMessage but this service isn't explicitly
    // given the actual type.  This means that incorrect usage will result in a runtime failure.
    this.codec = (MessageCodec<EntityMessage, ?>) owningEntity.getCodec();
    Assert.assertNotNull(codec);

    this.fakeDescriptor = EntityDescriptor.createDescriptorForInvoke(new FetchID(owningEntity.getConsumerID()),ClientInstanceID.NULL_ID);
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // Make sure we have started.
    scheduleMessage(message);
  }

  @Override
  public ExplicitRetirementHandle deferRetirement(String tag,
                                                  EntityMessage originalMessageToDefer,
                                                  EntityMessage futureMessage) {
    // defer, as normal
    retirementManager.deferRetirement(originalMessageToDefer, futureMessage);
    // return handle
    return new Handle(tag, futureMessage);
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer,
                                            EntityMessage newMessageToSchedule) throws MessageCodecException {
    // This requires that we access the RetirementManager to change the retirement of the current message.
    this.retirementManager.deferRetirement(originalMessageToDefer, newMessageToSchedule);
    // Schedule the message, as per normal.
    scheduleMessage(newMessageToSchedule);
  }

  @Override
  public ScheduledToken messageSelfAfterDelay(EntityMessage message,
                                              long millisBeforeSend) throws MessageCodecException {
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    long startTimeMillis = this.timer.currentTimeMillis() + millisBeforeSend;
    Runnable delayedRunnable = new Runnable() {
      @Override
      public void run() {
        // Pre-filter if entity was destroyed.
        if (!EntityMessengerService.this.owningEntity.isDestroyed()) {
          EntityMessengerService.this.messageSink.addSingleThreaded(interEntityMessage);
        }
      }
    };

    TokenID token = cacheEarlyInvoke(delayedRunnable, null, startTimeMillis, 0);
    if (token == null) {
      long id = this.timer.addDelayed(delayedRunnable, startTimeMillis);
      Assert.assertTrue(id > 0L);
      token = new TokenWrapper(id);
      addLiveToken(token);
    }
    return token;
  }
  
  private synchronized EarlyInvokeWrapper cacheEarlyInvoke(Runnable delayedRunnable,
                              SelfDestructiveRunnable periodicRunnable,
                              long startTimeMillis,
                              long repeatPeriodMillis) {
    if (owningEntity.isDestroyed()) {
      EarlyInvokeWrapper wrapper = new EarlyInvokeWrapper(delayedRunnable, periodicRunnable, startTimeMillis, repeatPeriodMillis);
      this.liveTokens.add(wrapper);
      return wrapper;
    } else {
      return null;
    }
  }
  
  private synchronized void addLiveToken(TokenID token) {
    this.liveTokens.add(token);
  }
  
  private synchronized boolean removeLiveToken(TokenID token) {
    return this.liveTokens.remove(token);
  }
  
  @Override
  public ScheduledToken messageSelfPeriodically(EntityMessage message,
                                                long millisBetweenSends) throws MessageCodecException {
    if (millisBetweenSends <= 0) {
      throw new IllegalArgumentException("Period of message send must be greater than 0 milliseconds");
    }
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    long startTimeMillis = this.timer.currentTimeMillis() + millisBetweenSends;
    SelfDestructiveRunnable runnable = new SelfDestructiveRunnable(this.messageSink,
                                                                   this.owningEntity,
                                                                   interEntityMessage);

    TokenID token = cacheEarlyInvoke(null, runnable, startTimeMillis, millisBetweenSends);
    if (token == null) {
      long id = this.timer.addPeriodic(runnable, startTimeMillis, millisBetweenSends);
      Assert.assertTrue(id > 0L);
      runnable.prepareForCancel(this.timer, token);
      token = new TokenWrapper(id);
      addLiveToken(token);
    }
    return token;
  }

  @Override
  public void cancelTimedMessage(ScheduledToken token) {
    if (removeLiveToken((TokenID) token)) {
      // If this is the wrong type, the ClassCastException is a reasonable error since it means we got something invalid.
      // Note that we ignore whether or not the cancel succeeded but we may want to log this, in the future.
      long realToken = ((TokenID) token).getToken();
      if (realToken > 0) {
        this.timer.cancel(realToken);
      }
    }
  }

  @Override
  public synchronized void entityCreated(CommonServerEntity sender) {
    // Walk our cache, flush it to the timer, and null it out to switch our mode.
    for (TokenID t : this.liveTokens) {
      if (t instanceof EarlyInvokeWrapper) {
        EarlyInvokeWrapper invoke = (EarlyInvokeWrapper)t;
        // Note that this is the token we already handed back to the entity so we can just modify this instance.
        if (null != invoke.delayedRunnable) {
          // One-time call.
          long id = this.timer.addDelayed(invoke.delayedRunnable, invoke.startTimeMillis);
          Assert.assertTrue(id > 0L);
          invoke.setToken(id);
        } else {
          Assert.assertNotNull(invoke.periodicRunnable);
          Assert.assertTrue(invoke.repeatPeriodMillis > 0L);
          // Periodic call.
          SelfDestructiveRunnable runnable = invoke.periodicRunnable;
          long id = this.timer.addPeriodic(runnable, invoke.startTimeMillis, invoke.repeatPeriodMillis);
          Assert.assertTrue(id > 0L);
          invoke.setToken(id);
          runnable.prepareForCancel(this.timer, invoke);
        }
      }
    }
  }

  @Override
  public synchronized void entityDestroyed(CommonServerEntity sender) {
    this.liveTokens.forEach((t)->timer.cancel(t.getToken()));
    this.liveTokens.clear();
  }

  private void scheduleMessage(EntityMessage message) throws MessageCodecException {
    // We first serialize the message (note that this is partially so we can use the common message processor, which expects
    // to deserialize, but also because we may have to replicate the message to the passive).
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    // if the entity isDestroyed(), this message could be being sent during the create sequence
    this.messageSink.addSingleThreaded(interEntityMessage);
  }

  private FakeEntityMessage encodeAsFake(EntityMessage message) throws MessageCodecException {
    byte[] serializedMessage = this.codec.encodeMessage(message);
    FakeEntityMessage interEntityMessage = new FakeEntityMessage(this.fakeDescriptor, message, serializedMessage, waitForReceived);
    return interEntityMessage;
  }
  /**
   * We fake up a Voltron entity message to enqueue for the entity to process in the future.
   */
  private static class FakeEntityMessage implements VoltronEntityMessage {
    private static final AtomicLong NEXT_FAKE_TXN_ID = new AtomicLong();

    private final EntityDescriptor descriptor;
    private final EntityMessage identityMessage;
    private final byte[] message;
    private final boolean waitForReceived;

    public FakeEntityMessage(EntityDescriptor descriptor, EntityMessage identityMessage, byte[] message, boolean waitForReceived) {
      this.descriptor = descriptor;
      this.identityMessage = identityMessage;
      this.message = message;
      this.waitForReceived = waitForReceived;
    }

    @Override
    public ClientID getSource() {
      return ClientID.NULL_ID;
    }

    @Override
    public TransactionID getTransactionID() {
      return new TransactionID(NEXT_FAKE_TXN_ID.incrementAndGet());
    }

    @Override
    public EntityDescriptor getEntityDescriptor() {
      return this.descriptor;
    }

    @Override
    public boolean doesRequireReplication() {
      return true;
    }

    @Override
    public boolean doesRequestReceived() {
      return waitForReceived;
    }

    @Override
    public Type getVoltronType() {
      return Type.INVOKE_ACTION;
    }

    @Override
    public byte[] getExtendedData() {
      return this.message;
    }

    @Override
    public TransactionID getOldestTransactionOnClient() {
      return TransactionID.NULL_ID;
    }

    @Override
    public EntityMessage getEntityMessage() {
      return this.identityMessage;
    }
  }

  private class SelfDestructiveRunnable implements Runnable {
    private final Sink<VoltronEntityMessage> messageSink;
    private final ManagedEntity owningEntity;
    private final FakeEntityMessage message;
    private ISimpleTimer timer;
    private TokenID id;

    public SelfDestructiveRunnable(Sink<VoltronEntityMessage> messageSink,
                                   ManagedEntity owningEntity,
                                   FakeEntityMessage message) {
      this.messageSink = messageSink;
      this.owningEntity = owningEntity;
      this.message = message;
    }

    public void prepareForCancel(ISimpleTimer timer, TokenID id) {
      this.timer = timer;
      this.id = id;
    }

    @Override
    public void run() {
      if (this.owningEntity.isDestroyed()) {
        this.timer.cancel(id.getToken());
        removeLiveToken(id);
      } else {
        this.messageSink.addSingleThreaded(this.message);
      }
    }
  }

  private static class TokenWrapper implements TokenID {
    private final long token;

    public TokenWrapper(long token) {
      this.token = token;
    }

    @Override
    public long getToken() {
      return this.token;
    }
  }
  

  private interface TokenID extends ScheduledToken {
   long getToken();
  }  

  private static class EarlyInvokeWrapper implements TokenID {
    public final Runnable delayedRunnable;
    public final SelfDestructiveRunnable periodicRunnable;
    public final long startTimeMillis;
    public final long repeatPeriodMillis;
    public long token;

    public EarlyInvokeWrapper(Runnable delayedRunnable,
                              SelfDestructiveRunnable periodicRunnable,
                              long startTimeMillis,
                              long repeatPeriodMillis) {
      this.delayedRunnable = delayedRunnable;
      this.periodicRunnable = periodicRunnable;
      this.startTimeMillis = startTimeMillis;
      this.repeatPeriodMillis = repeatPeriodMillis;
    }

    public void setToken(long token) {
      this.token = token;
    }

    @Override
    public long getToken() {
      return this.token;
    }
  }

  public class Handle implements ExplicitRetirementHandle {
    private final String tag;
    private final EntityMessage futureMessage;
    private final long nowTimeNS;
    private final boolean active = true;

    private Handle(String tag, EntityMessage futureMessage) {
      this.tag = tag;
      this.futureMessage = futureMessage;
      this.nowTimeNS = System.nanoTime();
      retirementHandles.put(this, this);
    }

    @Override
    public String getTag() {
      return tag;
    }

    @Override
    public void release() throws MessageCodecException {
      if (retirementHandles.remove(this) != null) {
        EntityMessengerService.this.messageSelf(futureMessage);
      }
    }

    public boolean isActive() {
      return active;
    }

    public long getCreationTimeMS() {
      return TimeUnit.MILLISECONDS.convert(nowTimeNS, TimeUnit.NANOSECONDS);
    }

    @Override
    public String toString() {
      return "ExplicitRetirementHandle: { active=" + active + " tag=" + tag + " age=" + nowTimeNS + "ns";
    }
  }
}
