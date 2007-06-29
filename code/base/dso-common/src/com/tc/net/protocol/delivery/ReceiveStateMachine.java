/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();

  private final SynchronizedLong           received           = new SynchronizedLong(-1);
  private final SynchronizedInt            delayedAcks        = new SynchronizedInt(0);
  private final int                        maxDelayedAcks;
  private final OOOProtocolMessageDelivery delivery;
  private StateMachineRunner               runner;

  private String                           debugId            = "UNKNOWN";

  private static final boolean             debug              = false;

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
    return ((runner != null) ? runner.getEventsCount() : 0);
  }

  private class MessageWaitState extends AbstractState {

    public MessageWaitState() {
      super("MESSAGE_WAIT_STATE");
    }

    public void execute(OOOProtocolMessage msg) {
      if (msg.isSend()) {
        handleSendMessage(msg);
      } else {
        // these message should be handled at higher level
        Assert.inv(msg.isAck() || msg.isGoodbye());
        Assert.inv(false);
      }
    }

    private void handleSendMessage(OOOProtocolMessage msg) {
      final long r = msg.getSent();
      final long curRecv = received.get();
      if (r <= curRecv) {
        // we already got message
        debugLog("Received dup msg "+r);
        sendAck(curRecv);
        delayedAcks.set(0);
        return;
      } else if (r > (curRecv + 1)) {
        // message missed, resend ack, receive to resend message.
        debugLog("Received out of order msg "+r);
        sendAck(curRecv);
        delayedAcks.set(0);
        return;
      } else {
        Assert.inv(r == (curRecv + 1));
        putMessage(msg);
        ackIfNeeded(received.increment());
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

  private void ackIfNeeded(long next) {
    if ((delayedAcks.get() < maxDelayedAcks) && (getRunnerEventLength() > 0)) {
      delayedAcks.increment();
    } else {
       /*
       * saw IllegalStateException by AbstractTCNetworkMessage.checkSealed
       * when message sent to non-established transport by MessageTransportBase.send.
       * reset delayedAcks only ack can be sent.
       */
        if (sendAck(next)) {
          delayedAcks.set(0);
        } else {
          debugLog("Failed to send ack:"+next);
        }
    }
  }

  private boolean sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    Assert.inv(opm.getSessionId() > -1);
    return (delivery.sendMessage(opm));
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

  public SynchronizedLong getReceived() {
    return received;
  }

  public void setRunner(StateMachineRunner receive) {
    this.runner = receive;
  }
}
