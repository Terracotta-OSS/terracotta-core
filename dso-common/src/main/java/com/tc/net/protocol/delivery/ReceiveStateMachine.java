/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.UUID;

/**
 * State Machine handling message receive for OOO
 */
public class ReceiveStateMachine extends AbstractStateMachine {
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();
  private final int                        maxDelayedAcks;
  private final String                     debugId;
  private final OOOProtocolMessageDelivery delivery;
  private static final boolean             debug              = false;

  private long                             received           = -1;
  private int                              delayedAcks        = 0;

  public ReceiveStateMachine(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig, boolean isClient) {
    maxDelayedAcks = reconnectConfig.getMaxDelayAcks();
    this.debugId = (isClient) ? "CLIENT" : "SERVER";
    this.delivery = delivery;
  }

  @Override
  public synchronized void execute(OOOProtocolMessage msg) {
    getCurrentState().execute(msg);
  }

  @Override
  protected State initialState() {
    return MESSAGE_WAIT_STATE;
  }

  @Override
  public String toString() {
    return "CurrentState: " + getCurrentState() + "; Received: " + received + "; DelayedAcks: " + delayedAcks + "; "
           + super.toString();
  }

  private class MessageWaitState extends AbstractState {

    public MessageWaitState() {
      super("MESSAGE_WAIT_STATE");
    }

    @Override
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
    ++delayedAcks;
    if (delayedAcks >= maxDelayedAcks) {
      if (sendAck(next)) {
        delayedAcks = 0;
      } else {
        debugLog("Failed to send ack:" + next);
      }
    }
  }

  private boolean sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    Assert.inv(!opm.getSessionId().equals(UUID.NULL_ID));
    return (delivery.sendMessage(opm));
  }

  @Override
  public synchronized void reset() {
    received = -1;
    delayedAcks = 0;
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("Receiver-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
    }
  }

  public synchronized long getReceived() {
    return received;
  }

  // for testing purpose only
  synchronized boolean isClean() {
    return ((received == -1) && (delayedAcks == 0));
  }

}
