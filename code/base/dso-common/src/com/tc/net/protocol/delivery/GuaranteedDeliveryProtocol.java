/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.Sink;
import com.tc.net.protocol.TCNetworkMessage;

/**
 * This implements an asynchronous Once and only once protocol. Sent messages go out on the sent queue received messages
 * come in to the ProtocolMessageDelivery instance.
 */
class GuaranteedDeliveryProtocol implements DeliveryProtocol {
  private final StateMachineRunner send;
  private final StateMachineRunner receive;
  private final BoundedLinkedQueue sendQueue;

  public GuaranteedDeliveryProtocol(OOOProtocolMessageDelivery delivery, Sink workSink, BoundedLinkedQueue sendQueue) {
    this.send = new StateMachineRunner(new SendStateMachine(delivery, sendQueue), workSink);
    this.receive = new StateMachineRunner(new ReceiveStateMachine(delivery), workSink);
    this.sendQueue = sendQueue;
  }

  public void send(TCNetworkMessage message) {
    try {
      sendQueue.put(message);
      send.addEvent(new OOOProtocolEvent());
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void receive(OOOProtocolMessage protocolMessage) {
    if (protocolMessage.isSend() || protocolMessage.isAckRequest()) {
      receive.addEvent(new OOOProtocolEvent(protocolMessage));
    } else if (protocolMessage.isAck()) {
      send.addEvent(new OOOProtocolEvent(protocolMessage));
    } else {
      throw new AssertionError();
    }
  }

  public void start() {
    send.start();
    receive.start();
  }

  public void pause() {
    send.pause();
    receive.pause();
  }

  public void resume() {
    send.resume();
    receive.resume();
  }

  public void reset() {
    send.reset();
    receive.reset();
  }
  
  public short getSenderSessionId() {
    return(send.getSessionId());
  }
  
  public short getReceiverSessionId() {
    return(receive.getSessionId());
  }
}
