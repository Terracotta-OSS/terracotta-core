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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.ExplicitRetirementHandle;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.EntityContainerListener;

import java.util.function.Consumer;


public class PassthroughMessengerService implements IEntityMessenger, EntityContainerListener {
  private final PassthroughTimerThread timerThread;
  private final PassthroughServerProcess passthroughServerProcess;
  private final PassthroughRetirementManager retirementManager;
  private final PassthroughConnection pseudoConnection;
  private final DeferredEntityContainer entityContainer;
  private final String entityClassName;
  private final String entityName;
  
  public PassthroughMessengerService(PassthroughTimerThread timerThread, PassthroughServerProcess passthroughServerProcess, PassthroughConnection pseudoConnection, DeferredEntityContainer entityContainer, boolean chain, String entityClassName, String entityName) {
    this.timerThread = timerThread;
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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule, Consumer response) throws MessageCodecException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

  private void commonSendMessage(PassthroughMessage passthroughMessage) {
    boolean shouldWaitForSent = false;
    boolean shouldWaitForReceived = false;
    boolean shouldWaitForCompleted = false;
    boolean shouldWaitForRetired = false;
    boolean shouldBlockGetUntilRetire = false;
    this.pseudoConnection.invokeActionAndWaitForAcks(passthroughMessage, shouldWaitForSent, shouldWaitForReceived, shouldWaitForCompleted, shouldWaitForRetired, shouldBlockGetUntilRetire);
  }


  private class SelfCancellingRunnable implements Runnable {
    private final PassthroughMessage message;
    private Long token;
    
    public SelfCancellingRunnable(PassthroughMessage message) {
      this.message = message;
    }
    
    public void setToken(Long token) {
      this.token = token;
    }
    
    @Override
    public void run() {
      Assert.assertTrue(this.token > 0);
      // If the entity disappeared, we are implicitly cancelled.
      if (null != PassthroughMessengerService.this.entityContainer.getEntity()) {
        commonSendMessage(this.message);
      } else {
        PassthroughMessengerService.this.timerThread.cancelMessage(this.token);
        System.err.println("WARNING:  Cancelled PERIODIC delayed message to destroyed entity");
      }
    }
  }

  private static class DeferredInvocation {
    public final Runnable delayRunnable;
    public final SelfCancellingRunnable periodicRunnable;
    public final long delayMillis;
    
    public DeferredInvocation(Runnable delayRunnable, SelfCancellingRunnable periodicRunnable, long delayMillis) {
      this.delayRunnable = delayRunnable;
      this.periodicRunnable = periodicRunnable;
      this.delayMillis = delayMillis;
    }
  }
}
