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
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;


public class PassthroughMessengerService implements IEntityMessenger {
  private final PassthroughTimerThread timerThread;
  private final PassthroughServerProcess passthroughServerProcess;
  private final PassthroughConnection pseudoConnection;
  private final DeferredEntityContainer entityContainer;
  private final String entityClassName;
  private final String entityName;
  
  public PassthroughMessengerService(PassthroughTimerThread timerThread, PassthroughServerProcess passthroughServerProcess, PassthroughConnection pseudoConnection, DeferredEntityContainer entityContainer, String entityClassName, String entityName) {
    this.timerThread = timerThread;
    this.passthroughServerProcess = passthroughServerProcess;
    this.pseudoConnection = pseudoConnection;
    // Note that we hold the entity container to get the codec but this container is deferred so we hold onto it, instead of
    // the codec (which probably isn't set yet).
    this.entityContainer = entityContainer;
    this.entityClassName = entityClassName;
    this.entityName = entityName;
  }

  @Override
  public void messageSelf(EntityMessage message) throws MessageCodecException {
    // Serialize the message.
    PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    commonSendMessage(passthroughMessage);
  }

  @Override
  public void messageSelfAndDeferRetirement(EntityMessage originalMessageToDefer, EntityMessage newMessageToSchedule) throws MessageCodecException {
    // Serialize the message.
    @SuppressWarnings("unchecked")
    MessageCodec<EntityMessage, ?> codec = (MessageCodec<EntityMessage, ?>) this.entityContainer.codec;
    byte[] serializedMessage = codec.encodeMessage(newMessageToSchedule);
    // We use the invalid instance 0 since this is not a connected client.
    long clientInstanceID = 0;
    boolean shouldReplicateToPassives = true;
    PassthroughMessage passthroughMessage = PassthroughMessageCodec.createInvokeMessage(this.entityClassName, this.entityName, clientInstanceID, serializedMessage, shouldReplicateToPassives);
    this.passthroughServerProcess.sendMessageToActiveFromInsideActive(newMessageToSchedule, passthroughMessage);
  }

  @Override
  public ScheduledToken messageSelfAfterDelay(EntityMessage message, long millisBeforeSend) throws MessageCodecException {
    final PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    // We need to have the entity at this point.
    Assert.assertTrue(null != this.entityContainer.entity);
    long token = this.timerThread.scheduleAfterDelay(new Runnable(){
      @Override
      public void run() {
        // If the entity disappeared, we are implicitly cancelled.
        if (null != PassthroughMessengerService.this.entityContainer.entity) {
          commonSendMessage(passthroughMessage);
        } else {
          System.err.println("WARNING:  Cancelled SINGLE delayed message to destroyed entity");
        }
      }}, millisBeforeSend);
    return new TokenWrapper(token);
  }

  @Override
  public ScheduledToken messageSelfPeriodically(EntityMessage message, long millisBetweenSends) throws MessageCodecException {
    final PassthroughMessage passthroughMessage = makePassthroughMessage(message);
    // We need to have the entity at this point.
    Assert.assertTrue(null != this.entityContainer.entity);
    SelfCancellingRunnable runnable = new SelfCancellingRunnable(passthroughMessage);
    long token = this.timerThread.schedulePeriodically(runnable, millisBetweenSends);
    runnable.setToken(token);
    return new TokenWrapper(token);
  }

  @Override
  public void cancelTimedMessage(ScheduledToken token) {
    // If this is the wrong type, the ClassCastException is a reasonable error since it means we got something invalid.
    this.timerThread.cancelMessage(((TokenWrapper)token).token);
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
      if (null != PassthroughMessengerService.this.entityContainer.entity) {
        commonSendMessage(this.message);
      } else {
        PassthroughMessengerService.this.timerThread.cancelMessage(this.token);
        System.err.println("WARNING:  Cancelled PERIODIC delayed message to destroyed entity");
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
