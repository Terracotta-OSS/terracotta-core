/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  private final OOOProtocolMessageDelivery delivery;
  private final LinkedList                 outstandingMsgs       = new LinkedList();
  private final int                        sendWindow;
  private final boolean                    isClient;
  private final String                     debugId;

  private static final boolean             debug                 = false;
  private static final TCLogger            logger                = TCLogging.getLogger(SendStateMachine.class);

  private long                             sent                  = -1;
  private long                             acked                 = -1;
  private int                              outstandingCnt        = 0;
  private LinkedBlockingQueue              sendQueue;

  public SendStateMachine(OOOProtocolMessageDelivery delivery, ReconnectConfig reconnectConfig, boolean isClient) {
    this.delivery = delivery;
    // set sendWindow from tc.properties if exist. 0 to disable window send.
    sendWindow = reconnectConfig.getSendWindow();
    int queueCap = reconnectConfig.getSendQueueCapacity();
    this.sendQueueCap = (queueCap == 0) ? Integer.MAX_VALUE : queueCap;
    this.sendQueue = new LinkedBlockingQueue(this.sendQueueCap);
    this.isClient = isClient;
    this.debugId = (this.isClient) ? "CLIENT" : "SERVER";
  }

  @Override
  protected void basicResume() {
    switchToState(initialState());
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
        // this shall not, old ack
        Assert.failure("Received bad ack: " + ackedSeq + " expected >= " + acked);
      } else {
        logger.info("SENDER-" + debugId + "-" + delivery.getConnectionId() + "; AckSeq: " + ackedSeq + " Acked: "
                    + acked);

        while (ackedSeq > acked) {
          ++acked;
          removeMessage();
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
      if ((protocolMessage != null) && protocolMessage.isAck()) {
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

      if (protocolMessage == null || protocolMessage.isSend()) {
        // we can't send data present in the queue until we get an ACK
        return;
      }

      Assert.eval(protocolMessage.isAck());

      long ackedSeq = protocolMessage.getAckSequence();
      if (ackedSeq < acked) {
        Assert.eval("SENDER-" + debugId + "-" + delivery.getConnectionId() + ": AckSeq " + ackedSeq
                    + " should be greater than " + acked, ackedSeq >= acked);
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
      if (protocolMessage == null || protocolMessage.isSend()) {
        // waiting for ACK message only
        return;
      }

      if (protocolMessage.isAck()) {
        switchToState(ACK_PROCESSING_STATE);
        getCurrentState().execute(protocolMessage);
      } else {
        Assert.failure("SEND_WINDOW_FULL_STATE doesn't expect this message: " + protocolMessage + ";\n" + this);
      }

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
    OOOProtocolMessage msg = (OOOProtocolMessage) outstandingMsgs.removeFirst();
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

    LinkedBlockingQueue tmpQ = sendQueue;
    sendQueue = new LinkedBlockingQueue(sendQueueCap);
    while (!tmpQ.isEmpty()) {
      dequeue(tmpQ);
    }
  }

  private static TCNetworkMessage dequeue(LinkedBlockingQueue q) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return (TCNetworkMessage) q.take();
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Util.selfInterruptIfNeeded(true);
      }
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
