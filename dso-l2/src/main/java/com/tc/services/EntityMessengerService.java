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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.handler.RetirementManager;
import com.tc.util.Assert;


/**
 * Implements the IEntityMessenger interface by maintaining a "fake" EntityDescriptor (as there is no actual reference from
 * a client) and using that to send "fake" VoltronEntityMessage instances into the server's message sink.
 */
public class EntityMessengerService implements IEntityMessenger {
  private final SingleThreadedTimer timer;
  private final Sink<VoltronEntityMessage> messageSink;
  private final ManagedEntity owningEntity;
  private final RetirementManager retirementManager;
  private final MessageCodec<EntityMessage, ?> codec;
  private final EntityDescriptor fakeDescriptor;

  @SuppressWarnings("unchecked")
  public EntityMessengerService(SingleThreadedTimer timer, Sink<VoltronEntityMessage> messageSink, ManagedEntity owningEntity) {
    Assert.assertNotNull(timer);
    Assert.assertNotNull(messageSink);
    Assert.assertNotNull(owningEntity);
    
    this.timer = timer;
    this.messageSink = messageSink;
    this.owningEntity = owningEntity;
    // We need access to the retirement manager in order to build dependencies between messages on this entity.
    this.retirementManager = owningEntity.getRetirementManager();
    // If this service is being created, we expect that the entity has a retirement mananger.
    Assert.assertTrue(null != this.retirementManager);
    // Note that the codec will actually expect to work on a sub-type of EntityMessage but this service isn't explicitly
    // given the actual type.  This means that incorrect usage will result in a runtime failure.
    this.codec = (MessageCodec<EntityMessage, ?>) owningEntity.getCodec();
    
    this.fakeDescriptor = EntityDescriptor.createDescriptorForLifecycle(owningEntity.getID(), owningEntity.getVersion());
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    scheduleMessage(message);
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule) throws MessageCodecException {
    // This requires that we access the RetirementManager to change the retirement of the current message.
    this.retirementManager.deferRetirement(originalMessageToDefer, newMessageToSchedule);
    // Schedule the message, as per normal.
    scheduleMessage(newMessageToSchedule);
  }

  @Override
  public ScheduledToken messageSelfAfterDelay(EntityMessage message, long millisBeforeSend) throws MessageCodecException {
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    long startTimeMillis = this.timer.currentTimeMillis() + millisBeforeSend;
    long id = this.timer.addDelayed(new Runnable() {
      @Override
      public void run() {
        // Pre-filter if entity was destroyed.
        if (!EntityMessengerService.this.owningEntity.isDestroyed()) {
          EntityMessengerService.this.messageSink.addSingleThreaded(interEntityMessage);
        }
      }}, startTimeMillis);
    
    return new TokenWrapper(id);
  }

  @Override
  public ScheduledToken messageSelfPeriodically(EntityMessage message, long millisBetweenSends) throws MessageCodecException {
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    long startTimeMillis = this.timer.currentTimeMillis() + millisBetweenSends;
    SelfDestructiveRunnable runnable = new SelfDestructiveRunnable(this.messageSink, this.owningEntity, interEntityMessage);
    long id = this.timer.addPeriodic(runnable, startTimeMillis, millisBetweenSends);
    runnable.prepareForCancel(this.timer, id);
    return new TokenWrapper(id);
  }

  @Override
  public void cancelTimedMessage(ScheduledToken token) {
    // If this is the wrong type, the ClassCastException is a reasonable error since it means we got something invalid.
    // Note that we ignore whether or not the cancel succeeded but we may want to log this, in the future.
    this.timer.cancel(((TokenWrapper)token).token);
  }


  private void scheduleMessage(EntityMessage message) throws MessageCodecException {
    // We first serialize the message (note that this is partially so we can use the common message processor, which expects
    // to deserialize, but also because we may have to replicate the message to the passive).
    FakeEntityMessage interEntityMessage = encodeAsFake(message);
    this.messageSink.addSingleThreaded(interEntityMessage);
  }

  private FakeEntityMessage encodeAsFake(EntityMessage message) throws MessageCodecException {
    byte[] serializedMessage = this.codec.encodeMessage(message);
    FakeEntityMessage interEntityMessage = new FakeEntityMessage(this.fakeDescriptor, message, serializedMessage);
    return interEntityMessage;
  }


  /**
   * We fake up a Voltron entity message to enqueue for the entity to process in the future.
   */
  private static class FakeEntityMessage implements VoltronEntityMessage {
    private final EntityDescriptor descriptor;
    private final EntityMessage identityMessage;
    private final byte[] message;

    public FakeEntityMessage(EntityDescriptor descriptor, EntityMessage identityMessage, byte[] message) {
      this.descriptor = descriptor;
      this.identityMessage = identityMessage;
      this.message = message;
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
    private SingleThreadedTimer timer;
    private long id;
    
    public SelfDestructiveRunnable(Sink<VoltronEntityMessage> messageSink, ManagedEntity owningEntity, FakeEntityMessage message) {
      this.messageSink = messageSink;
      this.owningEntity = owningEntity;
      this.message = message;
    }
    
    public void prepareForCancel(SingleThreadedTimer timer, long id) {
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
    public final long token;
    public TokenWrapper(long token) {
      this.token = token;
    }
  }
}
