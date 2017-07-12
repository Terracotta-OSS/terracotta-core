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
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.api.ManagedEntity.CreateListener;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.util.Assert;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Implements the IEntityMessenger interface by maintaining a "fake" EntityDescriptor (as there is no actual reference from
 * a client) and using that to send "fake" VoltronEntityMessage instances into the server's message sink.
 */
public class EntityMessengerService implements IEntityMessenger, CreateListener {
  private final ISimpleTimer timer;
  private final Sink<VoltronEntityMessage> messageSink;
  private final ManagedEntity owningEntity;
  private final boolean waitForReceived;
  private final RetirementManager retirementManager;
  private final MessageCodec<EntityMessage, ?> codec;
  private final EntityDescriptor fakeDescriptor;
  private Map<TokenWrapper, EarlyInvokeWrapper> earlyInvokeCache;
  private ConcurrentHashMap<ExplicitRetirementHandle, Handle> retirementHandles = new ConcurrentHashMap<>();

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

    this.fakeDescriptor = EntityDescriptor.createDescriptorForLifecycle(owningEntity.getID(),
                                                                        owningEntity.getVersion());

    // If the entity isn't already created, register to be notified when it is.
    if (owningEntity.isDestroyed()) {
      // The entity is still in the process of being built so we need to build our local cache and register for the
      // callback that it is running.
      // Note that the only case where this is true is when we were created during the initialization of the
      // owningEntity, meaning we are running in the same thread.  The possibility of racy situations, later on, should
      // only be possible if the entity has already been destroyed, in which case this isn't running.
      this.earlyInvokeCache = new HashMap<>();
      this.owningEntity.setSuccessfulCreateListener(this);
    }
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // Make sure we have started.
    checkCreationFinished();
    scheduleMessage(message);
  }

  @Override
  public ExplicitRetirementHandle deferRetirement(String tag,
                                                  EntityMessage originalMessageToDefer,
                                                  EntityMessage futureMessage) {
    // Make sure we have started.
    checkCreationFinished();
    // defer, as normal
    retirementManager.deferRetirement(originalMessageToDefer, futureMessage);
    // return handle
    return new Handle(tag, futureMessage);
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer,
                                            EntityMessage newMessageToSchedule) throws MessageCodecException {
    // Make sure we have started.
    checkCreationFinished();
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

    TokenWrapper token = null;
    if (null != this.earlyInvokeCache) {
      // Cache this until the entity is online.
      EarlyInvokeWrapper wrapper = new EarlyInvokeWrapper(delayedRunnable, null, startTimeMillis, 0);
      token = new TokenWrapper(TokenWrapper.UNINITIALIZED_TOKEN);
      this.earlyInvokeCache.put(token, wrapper);
    } else {
      long id = this.timer.addDelayed(delayedRunnable, startTimeMillis);
      Assert.assertTrue(id > 0L);
      token = new TokenWrapper(id);
    }
    return token;
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

    TokenWrapper token = null;
    if (null != this.earlyInvokeCache) {
      // Cache this until the entity is online.
      EarlyInvokeWrapper wrapper = new EarlyInvokeWrapper(null, runnable, startTimeMillis, millisBetweenSends);
      token = new TokenWrapper(TokenWrapper.UNINITIALIZED_TOKEN);
      this.earlyInvokeCache.put(token, wrapper);
    } else {
      long id = this.timer.addPeriodic(runnable, startTimeMillis, millisBetweenSends);
      Assert.assertTrue(id > 0L);
      runnable.prepareForCancel(this.timer, id);
      token = new TokenWrapper(id);
    }
    return token;
  }

  @Override
  public void cancelTimedMessage(ScheduledToken token) {
    if (null != this.earlyInvokeCache) {
      // If this is a token we handed out, it is instance-equal to the key of the cache.
      this.earlyInvokeCache.remove(token);
    } else {
      // If this is the wrong type, the ClassCastException is a reasonable error since it means we got something invalid.
      // Note that we ignore whether or not the cancel succeeded but we may want to log this, in the future.
      long realToken = ((TokenWrapper) token).getToken();
      Assert.assertTrue(realToken > 0L);
      this.timer.cancel(realToken);
    }
  }

  @Override
  public void entityCreationSucceeded(ManagedEntity sender) {
    // Walk our cache, flush it to the timer, and null it out to switch our mode.
    for (Map.Entry<TokenWrapper, EarlyInvokeWrapper> entry : this.earlyInvokeCache.entrySet()) {
      // Note that this is the token we already handed back to the entity so we can just modify this instance.
      TokenWrapper token = entry.getKey();
      EarlyInvokeWrapper invoke = entry.getValue();
      if (null != invoke.delayedRunnable) {
        // One-time call.
        long id = this.timer.addDelayed(invoke.delayedRunnable, invoke.startTimeMillis);
        Assert.assertTrue(id > 0L);
        token.setToken(id);
      } else {
        Assert.assertNotNull(invoke.periodicRunnable);
        Assert.assertTrue(invoke.repeatPeriodMillis > 0L);
        // Periodic call.
        SelfDestructiveRunnable runnable = invoke.periodicRunnable;
        long id = this.timer.addPeriodic(runnable, invoke.startTimeMillis, invoke.repeatPeriodMillis);
        Assert.assertTrue(id > 0L);
        token.setToken(id);
        runnable.prepareForCancel(this.timer, id);
      }
    }
    this.earlyInvokeCache = null;
  }

  private void scheduleMessage(EntityMessage message) throws MessageCodecException {
    // We first serialize the message (note that this is partially so we can use the common message processor, which expects
    // to deserialize, but also because we may have to replicate the message to the passive).
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    this.messageSink.addSingleThreaded(interEntityMessage);
  }

  private FakeEntityMessage encodeAsFake(EntityMessage message) throws MessageCodecException {
    byte[] serializedMessage = this.codec.encodeMessage(message);
    FakeEntityMessage interEntityMessage = new FakeEntityMessage(this.fakeDescriptor, message, serializedMessage, waitForReceived);
    return interEntityMessage;
  }

  private void checkCreationFinished() {
    if (null != this.earlyInvokeCache) {
      throw new IllegalStateException("Entity has not yet finished creation");
    }
  }

  /**
   * We fake up a Voltron entity message to enqueue for the entity to process in the future.
   */
  private static class FakeEntityMessage implements VoltronEntityMessage {
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
      return TransactionID.NULL_ID;
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

  private static class SelfDestructiveRunnable implements Runnable {
    private final Sink<VoltronEntityMessage> messageSink;
    private final ManagedEntity owningEntity;
    private final FakeEntityMessage message;
    private ISimpleTimer timer;
    private long id;

    public SelfDestructiveRunnable(Sink<VoltronEntityMessage> messageSink,
                                   ManagedEntity owningEntity,
                                   FakeEntityMessage message) {
      this.messageSink = messageSink;
      this.owningEntity = owningEntity;
      this.message = message;
    }

    public void prepareForCancel(ISimpleTimer timer, long id) {
      this.timer = timer;
      this.id = id;
    }

    @Override
    public void run() {
      if (this.owningEntity.isDestroyed()) {
        this.timer.cancel(id);
      } else {
        this.messageSink.addSingleThreaded(this.message);
      }
    }
  }

  private static class TokenWrapper implements ScheduledToken {
    public static long UNINITIALIZED_TOKEN = -1;

    private long token;

    public TokenWrapper(long token) {
      this.token = token;
    }

    public long getToken() {
      return this.token;
    }

    public void setToken(long token) {
      Assert.assertTrue(UNINITIALIZED_TOKEN == this.token);
      this.token = token;
    }
  }

  private static class EarlyInvokeWrapper {
    public final Runnable delayedRunnable;
    public final SelfDestructiveRunnable periodicRunnable;
    public final long startTimeMillis;
    public final long repeatPeriodMillis;

    public EarlyInvokeWrapper(Runnable delayedRunnable,
                              SelfDestructiveRunnable periodicRunnable,
                              long startTimeMillis,
                              long repeatPeriodMillis) {
      this.delayedRunnable = delayedRunnable;
      this.periodicRunnable = periodicRunnable;
      this.startTimeMillis = startTimeMillis;
      this.repeatPeriodMillis = repeatPeriodMillis;
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
