/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.properties.TCPropertiesImpl;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();

  private final SynchronizedLong           received           = new SynchronizedLong(-1);
  private final OOOProtocolMessageDelivery delivery;
  private int                              MaxDelayedAcks     = 16;                       // default by 16, can be set by
  // tc.properties, 0 to
  // disable.
  private final SynchronizedInt            delayedAcks        = new SynchronizedInt(0);

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery) {
    // set MaxDelayedAcks from tc.properties if exist. 0 to disable ack delay.
    String val = TCPropertiesImpl.getProperties().getProperty("l2.nha.ooo.maxDelayedAcks", true);
    if (val != null) MaxDelayedAcks = Integer.valueOf(val).intValue();
    this.delivery = delivery;
  }

  public void execute(OOOProtocolMessage msg) {
    getCurrentState().execute(msg);
  }

  protected State initialState() {
    return MESSAGE_WAIT_STATE;
  }

  private int getRunnerEventLength() {
    StateMachineRunner runner = getRunner();
    return ((runner != null) ? runner.getEventsCount() : 0);
  }

  private class MessageWaitState extends AbstractState {

    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage.isAckRequest()) {
        // acked with -1 for a fresh system, otherwise tell peer what I have got.
        delivery.sendAck(received.get());
        return;
      } else if (protocolMessage.isAck() && (protocolMessage.getAckSequence() == -1)) {
        // got ack=-1 then re-initialize
        reset();
        return;
      }

      final long r = protocolMessage.getSent();
      final long curRecv = received.get();
      if (r <= curRecv) {
        // we already got message
        resendAck();
      } else {
        if (r != (curRecv + 1)) {
          // message missed, resend ack, receive to resend message.
          resendAck();
          return;
        }
        putMessage(protocolMessage);
        sendAck();
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

  private void sendAck() {
    final long next = received.increment();
    if ((delayedAcks.get() < MaxDelayedAcks) && (getRunnerEventLength() > 0)) {
      delayedAcks.increment();
    } else {
      delivery.sendAck(next);
      delayedAcks.set(0);
    }
  }
  
  private void resendAck() {
    delivery.sendAck(received.get());
    delayedAcks.set(0);
  }

  public void reset() {
    received.set(-1);
    delayedAcks.set(0);
  }

}
