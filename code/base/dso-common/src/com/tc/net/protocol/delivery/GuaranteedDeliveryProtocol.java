/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.Sink;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.Assert;

/**
 * This implements an asynchronous Once and only once protocol. Sent messages go out on the sent queue received messages
 * come in to the ProtocolMessageDelivery instance.
 */
class GuaranteedDeliveryProtocol {
  private final StateMachineRunner  send;
  private final StateMachineRunner  receive;
  private final SendStateMachine    sender;
  private final ReceiveStateMachine receiver;

  public GuaranteedDeliveryProtocol(OOOProtocolMessageDelivery delivery, Sink workSink, boolean isClient) {
    this.sender = new SendStateMachine(delivery, isClient);
    this.send = new StateMachineRunner(sender, workSink);
    this.receiver = new ReceiveStateMachine(delivery);
    this.receive = new StateMachineRunner(receiver, workSink);
    receiver.setRunner(receive);
  }

  public void send(TCNetworkMessage message) {
    try {
      sender.put(message);
      send.addEvent(new OOOProtocolEvent());
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  public void receive(OOOProtocolMessage msg) {
    if (msg.isSend()) {
      receive.addEvent(new OOOProtocolEvent(msg));
    } else if (msg.isAck() || msg.isHandshakeReplyOk() || msg.isHandshakeReplyFail()) {
      send.addEvent(new OOOProtocolEvent(msg));
    } else {
      Assert.inv(false);
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
  
  public boolean isPaused() {
    return (send.isPaused() && receive.isPaused());
  }

  public void resume() {
    send.resume();
    receive.resume();
  }

  public void reset() {
    send.reset();
    receive.reset();
  }

  public ReceiveStateMachine getReceiver() {
    return receiver;
  }

  public SendStateMachine getSender() {
    return sender;
  }

  public void setDebugId(String debugId) {
    receiver.setDebugId(debugId);
  }
}
