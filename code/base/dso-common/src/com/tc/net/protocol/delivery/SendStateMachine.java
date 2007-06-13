/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCAssertionError;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

/**
 * 
 */
public class SendStateMachine extends AbstractStateMachine {
  private final State                      ACK_REQUEST_STATE  = new AckRequestState();
  private final State                      ACK_WAIT_STATE     = new AckWaitState();
  private final State                      HANDSHAKE_STATE    = new HandshakeState();
  private final State                      MESSAGE_WAIT_STATE = new MessageWaitState();
  private final SynchronizedLong           sent               = new SynchronizedLong(-1);
  private final SynchronizedLong           acked              = new SynchronizedLong(-1);
  private final OOOProtocolMessageDelivery delivery;
  private final BoundedLinkedQueue         sendQueue;
  private final LinkedList                 outstandingMsgs    = new LinkedList();
  private final SynchronizedInt            outstandingCnt     = new SynchronizedInt(0);
  private int                              sendWindow         = 32;                       // default by 32, can be

  // changed by tc.properties

  public SendStateMachine(OOOProtocolMessageDelivery delivery, BoundedLinkedQueue sendQueue) {
    super();

    // set sendWindow from tc.properties if exist. 0 to disable window send.
    String val = TCPropertiesImpl.getProperties().getProperty("l2.nha.ooo.sendWindow", true);
    if (val != null) sendWindow = Integer.valueOf(val).intValue();
    
    this.delivery = delivery;
    this.sendQueue = sendQueue;
    
    setSessionId(newRandomSessionId());
  }
  
  protected void basicResume() {
    switchToState(ACK_REQUEST_STATE);
  }

  protected State initialState() {
     Assert.eval(MESSAGE_WAIT_STATE != null);
    return MESSAGE_WAIT_STATE;
  }

  public void execute(OOOProtocolMessage msg) {
    Assert.eval(isStarted());
    getCurrentState().execute(msg);
  }

  private class MessageWaitState extends AbstractState {

    public void enter() {
      execute(null);
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (!sendQueue.isEmpty()) {
        Assert.eval(protocolMessage == null);
        if ((sendWindow == 0) || (outstandingCnt.get() < sendWindow)) {
          sendMessage(createProtocolMessage(sent.increment()));
        }
        switchToState(ACK_WAIT_STATE);
      }
    }
  }

  private class AckRequestState extends AbstractState {
    public void enter() {
        sendAckRequest();
        switchToState(HANDSHAKE_STATE);
    }
  }

  private class HandshakeState extends AbstractState {

    public void execute(OOOProtocolMessage protocolMessage) {
      // expecting an ack to do hand shake
      if (protocolMessage == null) return;
      if (protocolMessage.isSend()) return;
      // accept only matched sessionId
      if (!matchSessionId(protocolMessage)) {
        //System.err.println("XXX sender received unmatched ack expect "+ getSessionId() + " but got "+protocolMessage.getSessionId() );
        return;
      }
      long ackedSeq = protocolMessage.getAckSequence();
      
      if (ackedSeq == -1) {
        // System.out.println("XXX sender received ack -1");
        switchToState(MESSAGE_WAIT_STATE);
        return;
      }
      if (ackedSeq < acked.get()) {
        // this shall not, old ack
        throw new TCAssertionError("Wrong ack "+ackedSeq+" received! Expected >= "+acked.get());
      } else {
        while (ackedSeq > acked.get()) {
          acked.increment();
          removeMessage();
        }
        // resend outstanding which is not acked
        if (outstandingCnt.get() > 0) {
          // resend those not acked
          resendOutstandings();
          switchToState(ACK_WAIT_STATE);
        } else {
          // all acked, we're good here
          switchToState(MESSAGE_WAIT_STATE);
        }
      }
    }
  }

  private class AckWaitState extends AbstractState {

    public void enter() {
      sendMoreIfAvailable();
    }

    public void execute(OOOProtocolMessage protocolMessage) {
      if (protocolMessage == null || protocolMessage.isSend()) return;

      // accept only matched sessionId
      if (!matchSessionId(protocolMessage)) {
        //System.err.println("XXX sender received unmatched ack expect "+ getSessionId() + " but got "+protocolMessage.getSessionId() );
        return;
      }

      long ackedSeq = protocolMessage.getAckSequence();
      Assert.eval(ackedSeq >= acked.get());
      
      while (ackedSeq > acked.get()) {
        acked.increment();
        removeMessage();
      }

      // try pump more
      sendMoreIfAvailable();

      if (outstandingCnt.get() == 0) {
        switchToState(MESSAGE_WAIT_STATE);
      }

      // ???: is this check properly synchronized?
      Assert.eval(acked.get() <= sent.get());
    }

    public void sendMoreIfAvailable() {
      while ((outstandingCnt.get() < sendWindow) && !sendQueue.isEmpty()) {
        sendMessage(createProtocolMessage(sent.increment()));
      }
    }
  }

  private void sendAckRequest() {
    OOOProtocolMessage opm = delivery.createAckRequestMessage();
    // attached sessionId to ackRequest
    opm.setSessionId(getSessionId());
    sendMessage(opm);
  }

  private void sendMessage(OOOProtocolMessage protocolMessage) {
    delivery.sendMessage(protocolMessage);
  }

  private OOOProtocolMessage createProtocolMessage(long count) {
    OOOProtocolMessage opm;
    try {
      opm = delivery.createProtocolMessage(count, (TCNetworkMessage) sendQueue.take());
      // attached sessionId to each message from this sender
      opm.setSessionId(getSessionId());
      Assert.eval(opm != null);
      outstandingCnt.increment();
      outstandingMsgs.add(opm);
    } catch (InterruptedException ex) {
      throw new AssertionError(ex);
    }
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
    outstandingCnt.decrement();
  }

  public void reset() {
    // have a new sessionId to drop all old messages
    setSessionId(newRandomSessionId());

    sent.set(-1);
    acked.set(-1);

    // purge out outstanding sends
    outstandingCnt.set(0);
    outstandingMsgs.clear();

    while (!sendQueue.isEmpty()) {
      dequeue(sendQueue);
    }
  }

  private Object dequeue(BoundedLinkedQueue q) {
    try {
      return q.take();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }
  
  private short newRandomSessionId() {
    // generate a random session id
    Random r = new Random( ); 
    r.setSeed(System.currentTimeMillis());
    return((short) r.nextInt(0x0000fff0));
  }
  
}
