/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  private volatile long                    received           = -1;
  private volatile long                    lastAcked          = -1;

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
    return "CurrentState: " + getCurrentState() + "; Received: " + received + "; lastAcked: " + lastAcked + "; "
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
        throw new AssertionError();
      }
    }

    private void handleSendMessage(OOOProtocolMessage msg) {
      final long r = msg.getSent();
      if (r <= received) {
        // we already got message
        debugLog("Received dup msg " + r);
        sendAck(received);
      } else if (r > (received + 1)) {
        // message missed, resend ack, receive to resend message.
        debugLog("Received out of order msg " + r);
        sendAck(received);
      } else {
        Assert.assertTrue(r == (received + 1));
        putMessage(msg);
        ackIfNeeded(received = r);
      }
    }
  }

  private void putMessage(OOOProtocolMessage msg) {
    this.delivery.receiveMessage(msg);
  }

  private void ackIfNeeded(long next) {
    if (next - lastAcked >= maxDelayedAcks) {
      if (!sendAck(next)) {
        debugLog("Failed to send ack:" + next);
      }
    }
  }

  private boolean sendAck(long seq) {
    OOOProtocolMessage opm = delivery.createAckMessage(seq);
    Assert.assertTrue(!opm.getSessionId().equals(UUID.NULL_ID));
    if (delivery.sendMessage(opm)) {
      lastAcked = seq;
      return true;
    } else {
      return false;
    }
  }

  public long ackSequence() {
    // This is inherently a bit racey; on the SendStateMachine side (receiver for this ack), acks will arrive out of order
    // but that should be fine, as all we need to do is clean out the send window up to the highest received ack, essentially
    // ignore everything less than the highest ack seen.
    return (lastAcked = received);
  }

  @Override
  public synchronized void reset() {
    received = -1;
    lastAcked = -1;
  }

  private void debugLog(String msg) {
    if (debug) {
      DebugUtil.trace("Receiver-" + debugId + "-" + delivery.getConnectionID() + " -> " + msg);
    }
  }

  public synchronized long getReceived() {
    return received;
  }

  // for testing purpose only
  synchronized boolean isClean() {
    return ((received == -1) && (lastAcked == -1));
  }
}
