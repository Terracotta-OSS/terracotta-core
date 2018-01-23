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
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityException;

/**
 * Implements the IEntityMessenger interface by maintaining a "fake" EntityDescriptor (as there is no actual reference from
 * a client) and using that to send "fake" VoltronEntityMessage instances into the server's message sink.
 */
public class EntityMessengerService<M extends EntityMessage, R extends EntityResponse> implements IEntityMessenger<M, R>, LifecycleListener {
  private final AtomicLong NEXT_FAKE_TXN_ID = new AtomicLong();

  private final Sink<VoltronEntityMessage> messageSink;
  private final boolean waitForReceived;
  private final RetirementManager retirementManager;
  private final MessageCodec<M, R> codec;
  private final EntityDescriptor fakeDescriptor;
  private final ConcurrentHashMap<ExplicitRetirementHandle, Handle> retirementHandles = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public EntityMessengerService(Sink<VoltronEntityMessage> messageSink,
                                ManagedEntity owningEntity, boolean waitForReceived) {
    Assert.assertNotNull(messageSink);
    Assert.assertNotNull(owningEntity);

    this.messageSink = messageSink;
    this.waitForReceived = waitForReceived;
    // We need access to the retirement manager in order to build dependencies between messages on this entity.
    this.retirementManager = owningEntity.getRetirementManager();
    // If this service is being created, we expect that the entity has a retirement mananger.
    Assert.assertTrue(null != this.retirementManager);
    // Note that the codec will actually expect to work on a sub-type of EntityMessage but this service isn't explicitly
    // given the actual type.  This means that incorrect usage will result in a runtime failure.
    this.codec = (MessageCodec<M, R>) owningEntity.getCodec();
    Assert.assertNotNull(codec);

    this.fakeDescriptor = EntityDescriptor.createDescriptorForInvoke(new FetchID(owningEntity.getConsumerID()),ClientInstanceID.NULL_ID);
  }

  @Override
  public void messageSelf(M message) throws MessageCodecException {
    this.messageSelf(message, null);
  }

  @Override
  public void messageSelf(M message, Consumer<MessageResponse<R>> response) throws MessageCodecException {
    // Make sure we have started.
    scheduleMessage(message, response);
  }
  
  @Override
  public ExplicitRetirementHandle deferRetirement(String tag,
                                                  M originalMessageToDefer,
                                                  M futureMessage) {
    // defer, as normal
    retirementManager.deferRetirement(originalMessageToDefer, futureMessage);
    // return handle
    return new Handle(tag, futureMessage);
  }
  
  @Override
  public void messageSelfAndDeferRetirement(M originalMessageToDefer,
                                            M newMessageToSchedule) throws MessageCodecException {
    this.messageSelfAndDeferRetirement(originalMessageToDefer, newMessageToSchedule, null);
  }  

  @Override
  public void messageSelfAndDeferRetirement(M originalMessageToDefer,
                                            M newMessageToSchedule, Consumer<MessageResponse<R>> response) throws MessageCodecException {
    // This requires that we access the RetirementManager to change the retirement of the current message.
    this.retirementManager.deferRetirement(originalMessageToDefer, newMessageToSchedule);
    // Schedule the message, as per normal.
    scheduleMessage(newMessageToSchedule, response);
  }
  
  @Override
  public synchronized void entityCreated(ManagedEntity sender) {

  }

  @Override
  public synchronized void entityDestroyed(ManagedEntity sender) {

  }

  private void scheduleMessage(M message, Consumer<MessageResponse<R>> response) throws MessageCodecException {
    // We first serialize the message (note that this is partially so we can use the common message processor, which expects
    // to deserialize, but also because we may have to replicate the message to the passive).
    FakeEntityMessage interEntityMessage = encodeAsFake(message, response);
    // if the entity isDestroyed(), this message could be being sent during the create sequence
    this.messageSink.addToSink(interEntityMessage);
  }

  private FakeEntityMessage encodeAsFake(M message, Consumer<MessageResponse<R>> response) throws MessageCodecException {
    byte[] serializedMessage = this.codec.encodeMessage(message);
    FakeEntityMessage interEntityMessage = new FakeEntityMessage(this.fakeDescriptor, message, serializedMessage, response, waitForReceived);
    return interEntityMessage;
  }
  /**
   * We fake up a Voltron entity message to enqueue for the entity to process in the future.
   */
  public class FakeEntityMessage<R extends EntityResponse> implements VoltronEntityMessage {
    private final EntityDescriptor descriptor;
    private final EntityMessage identityMessage;
    private final byte[] message;
    private final Consumer<MessageResponse<R>> response;
    private final boolean waitForReceived;

    public FakeEntityMessage(EntityDescriptor descriptor, EntityMessage identityMessage, byte[] message, Consumer<MessageResponse<R>> response, boolean waitForReceived) {
      this.descriptor = descriptor;
      this.identityMessage = identityMessage;
      this.message = message;
      this.response = response;
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
    
    public Consumer<byte[]> getCompletionHandler() {
      return response == null ? null : (raw)->this.response.accept(new MessageResponse() {
        @Override
        public boolean wasExceptionThrown() {
          return false;
        }

        @Override
        public Exception getException() {
          return null;
        }

        @Override
        public EntityResponse getResponse() {
          try {
            return codec.decodeResponse(raw);
          } catch (MessageCodecException codec) {
            throw new RuntimeException(codec);
          }
        }
      });
    }
    
    public Consumer<EntityException> getExceptionHandler() {
      return response == null ? null : (exception)->this.response.accept(new MessageResponse() {
        @Override
        public boolean wasExceptionThrown() {
          return true;
        }

        @Override
        public Exception getException() {
          return exception;
        }

        @Override
        public EntityResponse getResponse() {
          return null;
        }
      });
    }
  }

  public class Handle implements ExplicitRetirementHandle {
    private final String tag;
    private final M futureMessage;
    private final long nowTimeNS;
    private final boolean active = true;

    private Handle(String tag, M futureMessage) {
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

    @Override
    public void release(Consumer consumer) throws MessageCodecException {
      if (retirementHandles.remove(this) != null) {
        EntityMessengerService.this.messageSelf(futureMessage, consumer);
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
