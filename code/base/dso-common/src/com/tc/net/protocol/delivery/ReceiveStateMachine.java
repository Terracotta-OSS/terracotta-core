/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

/**
 * 
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();

  private long                             received           = -1;
  private int                              delayedAcks        = 0;
  private final int                        maxDelayedAcks;
  private final OOOProtocolMessageDelivery delivery;
  private StateMachineRunner               runner;

  private String                           debugId            = "UNKNOWN";

  private static final boolean             debug              = false;

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig) {
    // set MaxDelayedAcks from tc.properties if exist. 0 to disable ack delay.
    maxDelayedAcks = reconnectConfig.getMaxDelayAcks();
    this.delivery = delivery;
  }

  public synchronized void execute(OOOProtocolMessage msg) {
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
      final long curRecv = received;
      if (r <= curRecv) {
        // we already got message
        debugLog("Received dup msg " + r);
        sendAck(curRecv);
        delayedAcks = 0;
        return;
      } else if (r > (curRecv + 1)) {
        // message missed, resend ack, receive to resend message.
        debugLog("Received out of order msg " + r);
        sendAck(curRecv);
        delayedAcks = 0;
        return;
      } else {
        Assert.inv(r == (curRecv + 1));
        putMessage(msg);
        ackIfNeeded(++received);
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

  private void ackIfNeeded(long next) {
    if ((delayedAcks < maxDelayedAcks) && (getRunnerEventLength() > 0)) {
      ++delayedAcks;
    } else {
      /*
       * saw IllegalStateException by AbstractTCNetworkMessage.checkSealed when message sent to non-established
       * transport by MessageTransportBase.send. reset delayedAcks only ack can be sent.
       */
      if (sendAck(next)) {
        delayedAcks = 0;
      } else {
        debugLog("Failed to send ack:" + next);
      }
    }
  }

  private boolean sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    Assert.inv(opm.getSessionId() > -1);
    return (delivery.sendMessage(opm));
  }

  public synchronized void reset() {
    received = -1;
    delayedAcks = 0;
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("Receiver-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

  public void setDebugId(String debugId) {
    this.debugId = debugId;
  }

  public synchronized long getReceived() {
    return received;
  }

  public void setRunner(StateMachineRunner receive) {
    this.runner = receive;
  }

  // for testing purpose only
  boolean isClean() {
    return ((received == -1) && (delayedAcks == 0));
  }

}
