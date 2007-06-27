/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;

import junit.framework.TestCase;

/**
 * 
 */
public class ReceiveStateMachineTest extends TestCase {

  public void tests() throws Exception {
    LinkedQueue receiveQueue = new LinkedQueue();
    TestProtocolMessageDelivery delivery = new TestProtocolMessageDelivery(receiveQueue);
    ReceiveStateMachine rsm = new ReceiveStateMachine(delivery);
    TestProtocolMessage tpm = new TestProtocolMessage();
    tpm.isHandshake = true;

    rsm.start();

    tpm.msg = new PingMessage(new NullMessageMonitor());
    tpm.sent = 0;
    tpm.isSend = true;
 
    assertEquals(0, delivery.receivedMessageCount);
    //REceive message
    rsm.execute(tpm);
    int received = delivery.receivedMessageCount;
    assertTrue(delivery.receivedMessageCount > 0);
    assertTrue(receiveQueue.poll(0) != null);
    
    //Receive a second time
    rsm.execute(tpm);
    assertEquals(received, delivery.receivedMessageCount);
    assertTrue(receiveQueue.poll(0) == null);
  }
}