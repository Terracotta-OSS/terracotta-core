/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.Util;

/**
 * This implements an asynchronous Once and only once protocol. Sent messages go out on the sent queue received messages
 * come in to the ProtocolMessageDelivery instance.
 */
class GuaranteedDeliveryProtocol {
  private final SendStateMachine    sender;
  private final ReceiveStateMachine receiver;

  public GuaranteedDeliveryProtocol(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig,
                                    boolean isClient) {
    this.sender = new SendStateMachine(delivery, reconnectConfig, isClient);
    this.receiver = new ReceiveStateMachine(delivery, reconnectConfig, isClient);
  }

  public void send(TCNetworkMessage message) {
    boolean interrupted = false;
    try {
      do {
        try {
          sender.put(message);
          break;
        } catch (InterruptedException e) {
          interrupted = true;
        }
      } while (true);

      sender.execute(null);
    } finally {
      if (interrupted) {
        Util.selfInterruptIfNeeded(interrupted);
      }
    }
  }

  public void receive(OOOProtocolMessage msg) {
    if (msg.isSend()) {
      receiver.execute(msg);
    } else if (msg.isAck() || msg.isHandshakeReplyOk()) {
      sender.execute(msg);
    } else {
      Assert.eval("Unexpected OOO Msg: " + msg, false);
    }
  }

  public synchronized void start() {
    sender.start();
    receiver.start();
  }

  public synchronized void pause() {
    if (!sender.isPaused()) sender.pause();
    if (!receiver.isPaused()) receiver.pause();
  }

  public synchronized boolean isPaused() {
    return (sender.isPaused() && receiver.isPaused());
  }

  public synchronized void resume() {
    sender.resume();
    receiver.resume();
  }

  public synchronized void reset() {
    sender.reset();
    receiver.reset();
  }

  public ReceiveStateMachine getReceiver() {
    return receiver;
  }

  public SendStateMachine getSender() {
    return sender;
  }

  @Override
  public String toString() {
    return "SendStateMachine: " + sender + "; ReceiveStateMachine: " + receiver;
  }
}
