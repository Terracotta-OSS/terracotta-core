/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;

import java.util.function.Consumer;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityException;


public class PassthroughMessengerService implements IEntityMessenger<EntityMessage, EntityResponse> {
  private final PassthroughServerProcess passthroughServerProcess;
  private final PassthroughRetirementManager retirementManager;
  private final DeferredEntityContainer entityContainer;
  private final String entityClassName;
  private final String entityName;
    
  public PassthroughMessengerService(PassthroughTimerThread timerThread, PassthroughServerProcess passthroughServerProcess, DeferredEntityContainer entityContainer, boolean chain, String entityClassName, String entityName) {
    this.passthroughServerProcess = passthroughServerProcess;
    this.retirementManager = passthroughServerProcess.getRetirementManager();
    // Note that we hold the entity container to get the codec but this container is deferred so we hold onto it, instead of
    // the codec (which probably isn't set yet).
    this.entityContainer = entityContainer;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
  }

  @Override
  public void destroySelf() {
    try {
      this.passthroughServerProcess.destroy(entityClassName, entityName);
    } catch (EntityException ee) {
    // ignore
    }
  }

  @Override
  public void create(String entityClassName, String entityName, long version, byte[] config) {
    try {
      this.passthroughServerProcess.create(entityClassName, entityName, version, config);
    } catch (EntityException ee) {
    // ignore
    }
  }

  @Override
  public void reconfigureSelf(byte[] config) {
    try {
      this.passthroughServerProcess.reconfigure(entityClassName, entityName, 1L, config);
    } catch (EntityException ee) {
    // ignore
    }
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // Serialize the message.
    PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(message, passthroughMessage, null);
  }

  @Override
  public ExplicitRetirementHandle deferRetirement(final String tag,
                                                  EntityMessage originalMessageToDefer,
                                                  final EntityMessage futureMessage) {
    try {
      final PassthroughMessage futurePassThroughMessage = makePassthroughMessage(futureMessage);
      retirementManager.deferCurrentMessage(futureMessage);
      return new ExplicitRetirementHandle() {
        @Override
        public String getTag() {
          return tag;
        }

        @Override
        public void release(Consumer consumer) throws MessageCodecException {
          passthroughServerProcess.sendMessageToActiveFromInsideActive(futureMessage, futurePassThroughMessage, null);
        }

        @Override
        public void release() throws MessageCodecException {
          passthroughServerProcess.sendMessageToActiveFromInsideActive(futureMessage, futurePassThroughMessage, null);
        }
      };
    } catch (MessageCodecException e) {
      System.err.println("Codec error in explicit retirement: " + e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule) throws MessageCodecException {
    retirementManager.deferCurrentMessage(newMessageToSchedule);
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(newMessageToSchedule, makePassthroughMessage(newMessageToSchedule), null);
  }

  @Override
  public void messageSelf(EntityMessage message, Consumer<MessageResponse<EntityResponse>> response) throws MessageCodecException {
    // Serialize the message.
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(message, makePassthroughMessage(message), queueForComplete(response));
  }
  
  private Consumer<PassthroughMessage> queueForComplete(Consumer<MessageResponse<EntityResponse>> response) {
    if (response != null) {
      return (msg)->{
        try {
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          DataOutputStream dos = new DataOutputStream(bos);
          msg.populateStream(dos);
          dos.close();
          ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
          DataInputStream dis = new DataInputStream(bis);
          switch (msg.type) {
          case MONITOR_MESSAGE: 
          case MONITOR_EXCEPTION:
          case COMPLETE_FROM_SERVER:
          case EXCEPTION_FROM_SERVER:
            boolean success = msg.type != PassthroughMessage.Type.MONITOR_EXCEPTION && msg.type != PassthroughMessage.Type.EXCEPTION_FROM_SERVER;
            int len = dis.readInt();
            byte[] data = new byte[len];
            dis.readFully(data);
            response.accept(new MessageResponse<EntityResponse>() {
              @Override
              public boolean wasExceptionThrown() {
                return success;
              }

              @Override
              public Exception getException() {
                return (!success) ? PassthroughMessageCodec.deserializeExceptionFromArray(data) : null;
              }

              @Override
              public EntityResponse getResponse() {
                try {
                  return (success) ? entityContainer.codec.decodeResponse(data) : null;
                } catch (MessageCodecException io) {
                  throw new RuntimeException(io);
                }
            }
            });
          }
        } catch (IOException io) {
          throw new RuntimeException(io);
        }
      };
    }
    return null;
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule, Consumer response) throws MessageCodecException {
    retirementManager.deferCurrentMessage(newMessageToSchedule);
    PassthroughMessage passthroughMessage = makePassthroughMessage(newMessageToSchedule);
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(newMessageToSchedule, passthroughMessage, queueForComplete(response));
  }

  private PassthroughMessage makePassthroughMessage(EntityMessage message) throws MessageCodecException {
    @SuppressWarnings("unchecked")
    MessageCodec<EntityMessage, ?> codec = (MessageCodec<EntityMessage, ?>) this.entityContainer.codec;
    byte[] serializedMessage = codec.encodeMessage(message);
    // We use the invalid instance 0 since this is not a connected client.
    long clientInstanceID = 0;
    boolean shouldReplicateToPassives = true;
    PassthroughMessage passthroughMessage = PassthroughMessageCodec.createInvokeMessage(this.entityClassName, this.entityName, clientInstanceID, serializedMessage, shouldReplicateToPassives);
    return passthroughMessage;
  }
}
