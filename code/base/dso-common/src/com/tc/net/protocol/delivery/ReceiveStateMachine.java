/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.util.Assert;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                   MESSAGE_WAIT_STATE = new MessageWaitState();

  private final SynchronizedLong        received           = new SynchronizedLong(-1);
  private final OOOProtocolMessageDelivery delivery;

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery) {
    this.delivery = delivery;
  }

  public void execute(OOOProtocolMessage msg) {
    getCurrentState().execute(msg);
  }

  protected State initialState() {
    return MESSAGE_WAIT_STATE;
  }

  private class MessageWaitState extends AbstractState {

    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage.isAckRequest()) {
        delivery.sendAck(received.get());
        return;
      }

      final long r = protocolMessage.getSent();
      final long curRecv = received.get();
      Assert.eval(r >= curRecv);
      if (r == curRecv) {
        //do nothing we already got it
      } else {
        putMessage(protocolMessage);
        final long next = received.increment();
        delivery.sendAck(next);
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

}