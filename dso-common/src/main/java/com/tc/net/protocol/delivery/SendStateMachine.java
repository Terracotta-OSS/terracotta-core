/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.delivery;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.Util;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * State Machine handling message send for OOO
 */
public class SendStateMachine extends AbstractStateMachine {
  private final int                        sendQueueCap;
  final State                              HANDSHAKE_WAIT_STATE  = new HandshakeWaitState();
  final State                              MESSAGE_WAIT_STATE    = new MessageWaitState();
  final State                              ACK_PROCESSING_STATE  = new AckProcessingState();
  final State                              SENDWINDOW_FULL_STATE = new SendWindowFullState();
  final State                              PAUSED_STATE          = new PausedState();

  private final OOOProtocolMessageDelivery delivery;
  private final LinkedList<OOOProtocolMessage> outstandingMsgs       = new LinkedList<OOOProtocolMessage>();
  private final int                        sendWindow;
  private final boolean                    isClient;
  private final String                     debugId;

  private static final boolean             debug                 = false;
  private static final TCLogger            logger                = TCLogging.getLogger(SendStateMachine.class);

  private long                             sent                  = -1;
  private long                             acked                 = -1;
  private int                              outstandingCnt        = 0;
  private LinkedBlockingQueue<TCNetworkMessage> sendQueue;

  public SendStateMachine(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig, boolean isClient) {
    this.delivery = delivery;
    // set sendWindow from tc.properties if exist. 0 to disable window send.
    sendWindow = reconnectConfig.getSendWindow();
    int queueCap = reconnectConfig.getSendQueueCapacity();
    this.sendQueueCap = (queueCap == 0) ? Integer.MAX_VALUE : queueCap;
    this.sendQueue = new LinkedBlockingQueue<TCNetworkMessage>(this.sendQueueCap);
    this.isClient = isClient;
    this.debugId = (this.isClient) ? "CLIENT" : "SERVER";
  }

  @Override
  protected void basicResume() {
    switchToState(initialState());
  }

  @Override
  protected void basicPause() {
    switchToState(PAUSED_STATE);
  }

  @Override
  protected State initialState() {
    return HANDSHAKE_WAIT_STATE;
  }

  @Override
  public synchronized void execute(OOOProtocolMessage msg) {
    Assert.eval(isStarted());
    getCurrentState().execute(msg);
  }

  @Override
  public String toString() {
    return "CurrentState: " + getCurrentState() + "; OutStandingMsgsCount: " + outstandingCnt + "; Sent: " + sent
           + "; Acked: " + acked + "; " + super.toString();
  }

  @Override
  protected synchronized void switchToState(State state) {
    if (debug) debugLog("switching state: " + getCurrentState() + " ==> " + state);
    super.switchToState(state);
  }

  /**
   * Need to handhshake with the other end before proceeding any sending/receiving messages.
   */
  private class HandshakeWaitState extends AbstractState {

    public HandshakeWaitState() {
      super("HANDSHAKE_WAIT_STATE");
    }

    @Override
    public void execute(OOOProtocolMessage msg) {
      // don't process sentQueue messages until handshake done
      if (msg == null) { return; }

      if (!msg.isHandshakeReplyOk()) {
        logger.warn("Expecting only Handhake Reply messages. Dropping :" + msg + ";\n" + this);
        return;
      }

      long ackedSeq = msg.getAckSequence();
      if (ackedSeq == -1) {
        if (debug) debugLog("The other side new/restarted.");
        switchToState(MESSAGE_WAIT_STATE);
        return;
      }
      if (ackedSeq < acked) {
        if (debug) {
          debugLog("received an old ack: " + ackedSeq + " current " + acked);
        }
      } else {
        logger.info("SENDER-" + debugId + "-" + delivery.getConnectionId() + "; AckSeq: " + ackedSeq + " Acked: "
                    + acked);

        while (ackedSeq > acked) {
          ++acked;
          removeMessage();
        }
      }
      if (outstandingCnt > 0) {
        // resend those not acked
        resendOutstandings();
        if (outstandingCnt >= sendWindow) {
          switchToState(SENDWINDOW_FULL_STATE);
        } else {
          switchToState(MESSAGE_WAIT_STATE);
        }
      } else {
        // all acked, we're good here
        switchToState(MESSAGE_WAIT_STATE);
      }
    }
  }

  /**
   * Ready to send more messages.
   */
  private class MessageWaitState extends AbstractState {

    public MessageWaitState() {
      super("MESSAGE_WAIT_STATE");
    }

    @Override
    public void enter() {
      // trigger sending messages which are queued up
      execute(null);
    }

    @Override
    public void execute(OOOProtocolMessage protocolMessage) {
      if ((protocolMessage != null)) {
        switchToState(ACK_PROCESSING_STATE);
        getCurrentState().execute(protocolMessage);
      } else {
        sendMoreIfAvailable();
        if ((sendWindow > 0) && (outstandingCnt >= sendWindow)) {
          switchToState(SENDWINDOW_FULL_STATE);
        }
      }
    }
  }

  /**
   * This is a temporary state and we are not suppose to stay back in this state. Hence, this state's won't be getting
   * send messages from by Guaranteed Delivery Protocol.
   */
  private class AckProcessingState extends AbstractState {
    public AckProcessingState() {
      super("ACK_PROCESSING_STATE");
    }

    @Override
    public void execute(OOOProtocolMessage protocolMessage) {

      if (protocolMessage == null) {
        // we can't send data present in the queue until we get an ACK
        return;
      }

      long ackedSeq = protocolMessage.getAckSequence();
      if (ackedSeq < acked) {
        if (debug) {
          debugLog("received an old ACK " + ackedSeq + " current " + acked);
        }
      }

      while (ackedSeq > acked) {
        ++acked;
        removeMessage();
      }

      if (outstandingCnt < sendWindow) {
        switchToState(MESSAGE_WAIT_STATE);
      } else {
        switchToState(SENDWINDOW_FULL_STATE);
      }

    }
  }

  /**
   * We need an ACK message from other end, to move forward. When in this state, Guaranteed Delivery Protocol's send
   * messages are sent to the queue and not processed.
   */
  private class SendWindowFullState extends AbstractState {

    public SendWindowFullState() {
      super("SEND_WINDOW_FULL_STATE");
    }

    @Override
    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage == null) {
        return;
      }

      if (protocolMessage.isAck() || protocolMessage.isSend()) {
        switchToState(ACK_PROCESSING_STATE);
        getCurrentState().execute(protocolMessage);
      } else {
        Assert.failure("SEND_WINDOW_FULL_STATE doesn't expect this message: " + protocolMessage + ";\n" + this);
      }

    }
  }

  /**
   * Paused state entered after a disconnect. No messages can be sent and it won't automatically transition into
   * another state.
   */
  private class PausedState extends AbstractState {
    private PausedState() {
      super("PAUSED_STATE");
    }
  }

  // send all or till the window
  private void sendMoreIfAvailable() {
    while (((sendWindow <= 0) || (outstandingCnt < sendWindow)) && !sendQueue.isEmpty()) {
      delivery.sendMessage(createProtocolMessage(++sent));
    }
  }

  private OOOProtocolMessage createProtocolMessage(long count) {
    final OOOProtocolMessage opm = delivery.createProtocolMessage(count, dequeue(sendQueue));
    Assert.eval(opm != null);
    outstandingCnt++;
    outstandingMsgs.add(opm);
    return (opm);
  }

  private void resendOutstandings() {
    ListIterator it = outstandingMsgs.listIterator(0);
    while (it.hasNext()) {
      OOOProtocolMessage msg = (OOOProtocolMessage) it.next();
      delivery.sendMessage(msg);
    }
  }

  private void removeMessage() {
    OOOProtocolMessage msg = outstandingMsgs.removeFirst();
    msg.reallyDoRecycleOnWrite();
    outstandingCnt--;
    Assert.eval(outstandingCnt >= 0);
  }

  @Override
  public synchronized void reset() {

    sent = -1;
    acked = -1;

    // purge out outstanding sends
    outstandingCnt = 0;
    outstandingMsgs.clear();

    LinkedBlockingQueue<TCNetworkMessage> tmpQ = sendQueue;
    sendQueue = new LinkedBlockingQueue<TCNetworkMessage>(sendQueueCap);
    tmpQ.clear();
  }

  private static TCNetworkMessage dequeue(LinkedBlockingQueue<TCNetworkMessage> q) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return q.take();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      Util.selfInterruptIfNeeded(interrupted);
    }
  }

  public void put(TCNetworkMessage message) throws InterruptedException {
    sendQueue.put(message);
  }

  private void debugLog(String msg) {
    if (debug) DebugUtil.trace("SENDER-" + debugId + "-" + delivery.getConnectionId() + " -> " + msg);
  }

  // for testing purpose only
  boolean isClean() {
    return (sendQueue.isEmpty() && outstandingMsgs.isEmpty());
  }

}
