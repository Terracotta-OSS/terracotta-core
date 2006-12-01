/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;

import junit.framework.TestCase;

/**
 *
 */
public class SendStateMachineTest extends TestCase {
  public void tests() throws Exception {
    TestProtocolMessageDelivery delivery = new TestProtocolMessageDelivery(new LinkedQueue());
    LinkedQueue sendQueue = new LinkedQueue();
    SendStateMachine ssm = new SendStateMachine(delivery, sendQueue);
    ssm.start();
    ssm.resume();

    TestProtocolMessage tpm = new TestProtocolMessage(null, -1, -1);
    tpm.isSend = true;
    
    //SEND
    MessageMonitor monitor = new NullMessageMonitor();
    sendQueue.put(new PingMessage(monitor));
    ssm.execute(null);
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 0);
    delivery.clear();

    //Call send an extra time with nothing on the send queue
    ssm.execute(tpm);
    assertTrue(delivery.created == false);
    tpm.isSend = false;

    sendQueue.put(new PingMessage(monitor));
    sendQueue.put(new PingMessage(monitor));
    tpm.ack = 0;

    //ACK
    ssm.execute(tpm);
    ssm.execute(tpm);
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 1);

    //RESEND
    delivery.clear();
    tpm.ack = 0;
    ssm.execute(tpm);
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 1);

    tpm.ack = 1;
    ssm.execute(tpm);

    delivery.clear();

    //SEND
    ssm.execute(tpm);
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 2);

    ssm.pause();
    assertTrue(ssm.isPaused());

    delivery.clear();
    //test ack request
    ssm.resume();
    assertFalse(ssm.isPaused());
    assertTrue(!delivery.created);
    assertTrue(delivery.sentAckRequest);
  }
}