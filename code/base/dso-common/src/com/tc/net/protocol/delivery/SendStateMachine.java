/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.Assert;

/**
 * 
 */
public class SendStateMachine extends AbstractStateMachine {
  private final State                   ACK_REQUEST_STATE  = new AckRequestState();
  private final State                   ACK_WAIT_STATE     = new AckWaitState();
  private final State                   MESSAGE_WAIT_STATE = new MessageWaitState();
  private final SynchronizedLong        sent               = new SynchronizedLong(-1);
  private final SynchronizedLong         acked              = new SynchronizedLong(-1);
  private final OOOProtocolMessageDelivery delivery;
  private final LinkedQueue             sendQueue;

  public SendStateMachine(OOOProtocolMessageDelivery delivery, LinkedQueue sendQueue) {
    super();
    this.delivery = delivery;
    this.sendQueue = sendQueue;
  }

  protected void basicResume() {
    switchToState(ACK_REQUEST_STATE);
  }

  protected State initialState() {
    Assert.eval(MESSAGE_WAIT_STATE != null);
    return MESSAGE_WAIT_STATE;
  }

  public void execute(OOOProtocolMessage msg) {
    Assert.eval(isStarted());
    getCurrentState().execute(msg);
  }

  private class MessageWaitState extends AbstractState {

    public void enter() {
      execute(null);
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (!sendQueue.isEmpty()) {
        Assert.eval(protocolMessage == null);
        sendMessage(createProtocolMessage(sent.increment()));
        switchToState(ACK_WAIT_STATE);
      }
    }
  }

  private class AckRequestState extends AbstractState {
    public void enter() {
      if (sent.get() > acked.get()) {
        sendAckRequest();
        switchToState(ACK_WAIT_STATE);
      } else {
        switchToState(MESSAGE_WAIT_STATE);
      }

    }
  }

  private class AckWaitState extends AbstractState {
    public void execute(OOOProtocolMessage protocolMessage) {
      // double yuck
      if (protocolMessage == null) return;
      
      //yuck
      if (protocolMessage.isSend()) return;
      
      if (protocolMessage.getAckSequence() < sent.get()) {
        sendMessage(createProtocolMessage(sent.get()));
      } else {
        acked.set(protocolMessage.getAckSequence());
        removeMessage();
        switchToState(MESSAGE_WAIT_STATE);
       
        // ???: is this check properly synchronized?
        Assert.eval(acked.get() <= sent.get());
      }
    }
  }

  private void sendAckRequest() {
    delivery.sendAckRequest();
  }

  private void sendMessage(OOOProtocolMessage protocolMessage) {
    delivery.sendMessage(protocolMessage);
  }

  private OOOProtocolMessage createProtocolMessage(long count) {
    TCNetworkMessage tcm = (TCNetworkMessage) sendQueue.peek();
    Assert.eval(tcm != null);
    return delivery.createProtocolMessage(count, tcm);
  }

  private void removeMessage() {
    try {
      sendQueue.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }
}