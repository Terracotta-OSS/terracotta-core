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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.EntityContainerListener;

import java.util.function.Consumer;
import org.terracotta.entity.EntityResponse;


public class PassthroughMessengerService implements IEntityMessenger, EntityContainerListener {
  private final PassthroughServerProcess passthroughServerProcess;
  private final PassthroughRetirementManager retirementManager;
  private final PassthroughConnection pseudoConnection;
  private final DeferredEntityContainer entityContainer;
  private final String entityClassName;
  private final String entityName;
  
  public PassthroughMessengerService(PassthroughTimerThread timerThread, PassthroughServerProcess passthroughServerProcess, PassthroughConnection pseudoConnection, DeferredEntityContainer entityContainer, boolean chain, String entityClassName, String entityName) {
    this.passthroughServerProcess = passthroughServerProcess;
    this.retirementManager = passthroughServerProcess.getRetirementManager();
    this.pseudoConnection = pseudoConnection;
    // Note that we hold the entity container to get the codec but this container is deferred so we hold onto it, instead of
    // the codec (which probably isn't set yet).
    this.entityContainer = entityContainer;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
    
    // Since this service needs access to the entity, potentially before the entity constructor has returned, see if we
    // need to register for notifications that the entity has been created.
    if (null == this.entityContainer.getEntity()) {
      this.entityContainer.notifyOnEntitySet(this);
    }
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // Serialize the message.
    PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    commonSendMessage(passthroughMessage);
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
          passthroughServerProcess.sendMessageToActiveFromInsideActive(futureMessage, futurePassThroughMessage);
        }

        @Override
        public void release() throws MessageCodecException {
          passthroughServerProcess.sendMessageToActiveFromInsideActive(futureMessage, futurePassThroughMessage);
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
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(newMessageToSchedule,
        makePassthroughMessage(newMessageToSchedule));
  }

  @Override
  public void messageSelf(EntityMessage message, Consumer response) throws MessageCodecException {
    // Serialize the message.
    PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    Future<byte[]> answer = commonSendMessage(passthroughMessage);
    if (response != null) {
      try {
        byte[] data = answer.get();
        MessageCodec<EntityMessage, ?> codec = (MessageCodec<EntityMessage, ?>) this.entityContainer.codec;
        EntityResponse serializedMessage = codec.decodeResponse(data);
        response.accept(serializedMessage);
      } catch (InterruptedException | ExecutionException ie) {
        throw new RuntimeException(ie);
      }
    }
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule, Consumer response) throws MessageCodecException {
    retirementManager.deferCurrentMessage(newMessageToSchedule);
    PassthroughMessage passthroughMessage = makePassthroughMessage(newMessageToSchedule);
    Future<byte[]> answer = commonSendMessage(passthroughMessage);

    if (response != null) {
      try {
        byte[] data = answer.get();
        MessageCodec<EntityMessage, ?> codec = (MessageCodec<EntityMessage, ?>) this.entityContainer.codec;
        EntityResponse serializedMessage = codec.decodeResponse(data);
        response.accept(serializedMessage);
      } catch (InterruptedException | ExecutionException ie) {
        throw new RuntimeException(ie);
      }
    }
  }

  @Override
  public void entitySetInContainer(DeferredEntityContainer container) {

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

  private Future<byte[]> commonSendMessage(PassthroughMessage passthroughMessage) {
    boolean shouldWaitForSent = false;
    boolean shouldWaitForReceived = false;
    boolean shouldWaitForCompleted = false;
    boolean shouldWaitForRetired = false;
    boolean shouldBlockGetUntilRetire = false;
    boolean deferred = false;
    return this.pseudoConnection.invokeActionAndWaitForAcks(passthroughMessage, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, shouldBlockGetUntilRetire, deferred, null);
  }
}
