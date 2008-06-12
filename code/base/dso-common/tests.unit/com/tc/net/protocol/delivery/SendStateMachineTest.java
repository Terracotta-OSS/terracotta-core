/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import junit.framework.TestCase;

/**
 *
 */
public class SendStateMachineTest extends TestCase {
  public void tests() throws Exception {
    TestProtocolMessageDelivery delivery = new TestProtocolMessageDelivery(new LinkedQueue());
    final short sessionId = 134;
    final int sendQueueCap = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP);
    SendStateMachine ssm = new SendStateMachine(delivery, sendQueueCap, true);
    ssm.start();
    ssm.resume();

    // hand shake state
    // compelete hand shake, receive ack=-1 from receiver
    TestProtocolMessage msg = new TestProtocolMessage(null, 0, -1);
    msg.isAck = true;
    msg.setSessionId(sessionId);
    ssm.execute(msg);

    TestProtocolMessage tpm = new TestProtocolMessage(null, -1, -1);
    tpm.setSessionId(sessionId);
    tpm.isSend = true;

    // SEND
    MessageMonitor monitor = new NullMessageMonitor();
    ssm.put(new PingMessage(monitor));
    ssm.execute(null); // msg 0
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 0);
    delivery.clear();

    // Call send an extra time with nothing on the send queue
    ssm.execute(tpm); // drop
    assertTrue(delivery.created == false);
    tpm.isSend = false;

    ssm.put(new PingMessage(monitor)); // msg 1
    ssm.put(new PingMessage(monitor)); // msg 2
    tpm.ack = 0;

    // ACK
    ssm.execute(tpm); // ack 0
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 2); // msg 2 is the last send

    ssm.pause();
    assertTrue(ssm.isPaused());

    // HAND SHAKE for RESEND
    delivery.clear();
    ssm.resume();
    assertFalse(ssm.isPaused());

    tpm.ack = 0;
    ssm.execute(tpm); // dup ack=0

    ssm.put(new PingMessage(monitor)); // msg 3
    ssm.execute(null); 
    assertTrue(delivery.msg.getSent() == 3);
 
    tpm.ack = 2;
    ssm.execute(tpm); // ack 2
    assertTrue(delivery.msg.getSent() == 3);

  }
}
