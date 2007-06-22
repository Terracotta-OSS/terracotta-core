/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.properties.TCPropertiesImpl;
import com.tc.util.DebugUtil;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();

  private final SynchronizedLong           received           = new SynchronizedLong(-1);
  private final SynchronizedInt            delayedAcks        = new SynchronizedInt(0);
  private final OOOProtocolMessageDelivery delivery;
  private final int                        maxDelayedAcks;

  private String                           debugId            = "UNKNOWN";
  private static final boolean             debug              = true;

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery) {
    // set MaxDelayedAcks from tc.properties if exist. 0 to disable ack delay.
    maxDelayedAcks = TCPropertiesImpl.getProperties().getInt("l2.nha.ooo.maxDelayedAcks", 16);
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
        debugLog("Got AckRequest, localSessionId=" + getSessionId() + ", remoteSessionId="
                 + protocolMessage.getSessionId());
        if ((received.get() == -1)) {
          // handshake to a fresh start
          // set session id from ackRequest message
          debugLog("Adopting remote Session id");
          setSessionId(protocolMessage.getSessionId());
          sendAck(-1);
          return;
        } else if (matchSessionId(protocolMessage)) {
          // tell peer what I have got.
          sendAck(received.get());
          return;
        } else {
          // System.err.println("XXX receiver got unmatched ackRequest expect "+getSessionId()+" but got
          // "+protocolMessage.getSessionId());
          debugLog("Unmatched session id -> resetting Receiver and adopting remote session id");
          reset();
          setSessionId(protocolMessage.getSessionId());
          sendAck(-1);
          return;
        }
      } else if (protocolMessage.isAck() && (protocolMessage.getAckSequence() == -1)) {
        debugLog("THIS SHOULD NOT BE POSSIBLE! ERROR!!!");
        // got ack=-1 then re-initialize
        setSessionId(protocolMessage.getSessionId());
        reset();
        return;
      }

      // accept only matched sessionId
      if (!matchSessionId(protocolMessage)) {
        debugLog("Bad Session Id on Normal Message - dropping, localSessionId=" + getSessionId() + ", remoteSessionId="
                 + protocolMessage.getSessionId());
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
    if ((delayedAcks.get() < maxDelayedAcks) && (getRunnerEventLength() > 0)) {
      delayedAcks.increment();
    } else {
      sendAck(next);
      delayedAcks.set(0);
    }
  }

  private void resendAck() {
    sendAck(received.get());
    delayedAcks.set(0);
  }

  private void sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    // attached sessionId to ack message
    opm.setSessionId(getSessionId());
    delivery.sendMessage(opm);
  }

  public void reset() {
    received.set(-1);
    delayedAcks.set(0);
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("Receiver-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

  public void setDebugId(String debugId) {
    this.debugId = debugId;
  }

}
