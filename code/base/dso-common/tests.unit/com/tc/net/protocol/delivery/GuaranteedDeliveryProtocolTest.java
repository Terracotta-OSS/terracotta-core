/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.objectserver.api.TestSink;

import junit.framework.TestCase;

/**
 * 
 */
public class GuaranteedDeliveryProtocolTest extends TestCase {
  public void tests() throws Exception {
    LinkedQueue receiveQueue = new LinkedQueue();
    TestProtocolMessageDelivery delivery = new TestProtocolMessageDelivery(receiveQueue);
    TestSink workSink = new TestSink();
    BoundedLinkedQueue sendQueue = new BoundedLinkedQueue();

    GuaranteedDeliveryProtocol gdp = new GuaranteedDeliveryProtocol(delivery, workSink, sendQueue);
    gdp.start();
    gdp.resume();
    TCNetworkMessage tcMessage = new PingMessage(new NullMessageMonitor());
    assertTrue(workSink.size() == 0);
    gdp.send(tcMessage);
    assertTrue(workSink.size() == 1);
    runWorkSink(workSink);
    assertTrue(delivery.created);
    assertTrue(delivery.tcMessage == tcMessage);
    TestProtocolMessage pm = (TestProtocolMessage) delivery.msg;
    delivery.clear();
    pm.isSend = true;
    gdp.receive(pm);
    assertTrue(workSink.size() == 1);
    runWorkSink(workSink);

    assertTrue(receiveQueue.take() == tcMessage);
    assertTrue(delivery.sentAck);
    assertTrue(delivery.ackCount == 0);

    delivery.clear();
    TestProtocolMessage ackMessage = new TestProtocolMessage();
    ackMessage.ack = 0;
    ackMessage.isAck = true;
    gdp.receive(ackMessage);
    assertTrue(workSink.size() == 1);
    runWorkSink(workSink);
    delivery.clear();

    gdp.send(tcMessage);
    gdp.send(tcMessage);
    assertTrue(workSink.size() == 1);
    runWorkSink(workSink);
    assertTrue(workSink.size() == 1);
  }

  private void runWorkSink(TestSink sink) {
    StateMachineRunner smr = (StateMachineRunner) sink.getInternalQueue().remove(0);
    smr.run();
  }
}
