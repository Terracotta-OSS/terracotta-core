/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.objectserver.api.TestSink;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;

import junit.framework.TestCase;

/**
 * 
 */
public class GuaranteedDeliveryProtocolTest extends TestCase {
  LinkedQueue                         receiveQueue;
  private TestSink                    sendSink;
  private TestSink                    receiveSink;
  private TestProtocolMessageDelivery delivery;
  private GuaranteedDeliveryProtocol  gdp;

  public void setUp() {
    receiveQueue = new LinkedQueue();
    delivery = new TestProtocolMessageDelivery(receiveQueue);
    sendSink = new TestSink();
    receiveSink = new TestSink();

    final ReconnectConfig reconnectConfig = new L1ReconnectConfigImpl();
    gdp = new GuaranteedDeliveryProtocol(delivery, sendSink, receiveSink, reconnectConfig, true);
    gdp.start();
    gdp.resume();
  }

  public void tests() throws Exception {
    final short sessionId = 124;

    // hand shake state
    // send AckRequest to receiver
    TestProtocolMessage msg = new TestProtocolMessage();
    msg.isHandshake = true;
    msg.setSessionId(sessionId);
    gdp.receive(msg);
    runWorkSink(sendSink);
    // reply ack=-1 from receiver
    msg = new TestProtocolMessage(null, 0, -1);
    msg.isAck = true;
    msg.setSessionId(sessionId);
    gdp.receive(msg);
    runWorkSink(sendSink);

    TCNetworkMessage tcMessage = new PingMessage(new NullMessageMonitor());
    assertTrue(sendSink.size() == 0);
    gdp.send(tcMessage);
    assertTrue(sendSink.size() == 1);
    runWorkSink(sendSink);
    assertTrue(delivery.created);
    assertTrue(delivery.tcMessage == tcMessage);
    TestProtocolMessage pm = (TestProtocolMessage) delivery.msg;
    pm.setSessionId(sessionId);
    delivery.clear();
    pm.isSend = true;
    gdp.receive(pm);
    assertTrue(receiveSink.size() == 1);
    runWorkSink(receiveSink);

    assertTrue(receiveQueue.take() == tcMessage);
    assertTrue(delivery.sentAck);
    assertTrue(delivery.ackCount == 0);

    delivery.clear();
    TestProtocolMessage ackMessage = new TestProtocolMessage();
    ackMessage.setSessionId(sessionId);
    ackMessage.ack = 0;
    ackMessage.isAck = true;
    gdp.receive(ackMessage);
    assertTrue(sendSink.size() == 1);
    runWorkSink(sendSink);
    delivery.clear();

    gdp.send(tcMessage);
    gdp.send(tcMessage);
    assertTrue(sendSink.size() == 1);
    runWorkSink(sendSink);
    assertTrue(sendSink.size() == 1);
  }

  public void testTransportDisconnect() {
    final short sessionId = 125;
    SendStateMachine sender = gdp.getSender();
    ReceiveStateMachine receiver = gdp.getReceiver();
    
    // hand shake state
    // send AckRequest to receiver
    TestProtocolMessage msg = new TestProtocolMessage();
    msg.isHandshake = true;
    msg.setSessionId(sessionId);
    gdp.receive(msg);
    runWorkSink(sendSink);
    // reply ack=-1 from receiver
    msg = new TestProtocolMessage(null, 0, -1);
    msg.isAck = true;
    msg.setSessionId(sessionId);
    gdp.receive(msg);
    runWorkSink(sendSink);

    TCNetworkMessage tcMessage = new PingMessage(new NullMessageMonitor());
    assertTrue(sendSink.size() == 0);
    gdp.send(tcMessage);
    gdp.send(tcMessage);
    gdp.send(tcMessage);
    gdp.send(tcMessage);
    gdp.send(tcMessage);
    assertTrue(sendSink.size() == 1);
    runWorkSink(sendSink);
    assertTrue(sendSink.size() == 1);
    assertFalse(sender.isClean());
    TestProtocolMessage pm = new TestProtocolMessage(tcMessage, 0, 0);
    pm.isSend = true;
    gdp.receive(pm);
    pm = new TestProtocolMessage(tcMessage, 1, 1);
    pm.isSend = true;
    gdp.receive(pm);
    pm = new TestProtocolMessage(tcMessage, 2, 2);
    pm.isSend = true;
    gdp.receive(pm);
    runWorkSink(receiveSink);
    assertTrue(receiveSink.size() == 1);
    assertFalse(receiver.isClean());
    
    // simulate transport disconnected, call reset().
    gdp.reset();
    assertTrue(sender.isClean());
    assertTrue(receiver.isClean());
    runWorkSink(sendSink);
    runWorkSink(receiveSink);
    assertTrue(sendSink.size() == 0);
    assertTrue(receiveSink.size() == 0);
  }

  private void runWorkSink(TestSink sink) {
    StateMachineRunner smr = (StateMachineRunner) sink.getInternalQueue().remove(0);
    smr.run();
  }
}
