/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.UUID;

public class GuaranteedDeliveryProtocolTest extends TCTestCase {
  LinkedQueue                         clientReceiveQueue;
  LinkedQueue                         serverReceiveQueue;
  private TestProtocolMessageDelivery clientDelivery;
  private TestProtocolMessageDelivery serverDelivery;
  private GuaranteedDeliveryProtocol  serverGdp;
  private GuaranteedDeliveryProtocol  clientGdp;
  private ReconnectConfig             reconnectConfig;

  public GuaranteedDeliveryProtocolTest() {
    // disableAllUntil("2010-12-25");
  }

  public void setUp(ReconnectConfig reconnectCfg) {
    clientReceiveQueue = new LinkedQueue();
    serverReceiveQueue = new LinkedQueue();

    reconnectConfig = reconnectCfg;
    clientDelivery = new TestProtocolMessageDelivery(clientReceiveQueue);
    serverDelivery = new TestProtocolMessageDelivery(serverReceiveQueue);

    clientGdp = new GuaranteedDeliveryProtocol(clientDelivery, reconnectConfig, true);
    clientGdp.start();
    clientGdp.resume();

    serverGdp = new GuaranteedDeliveryProtocol(serverDelivery, reconnectConfig, true);
    serverGdp.start();
    serverGdp.resume();

  }

  public void testDefault() throws Exception {
    setUp(new L1ReconnectConfigImpl());
    oooTest();
  }

  public void testErrorConfig() throws Exception {
    try {
      setUp(new L1ReconnectConfigImpl(true, 5000, 5000, 1, 1));
    } catch (Exception e) {
      System.out.println("XXX Expected : " + e);
      return;
    }
    Assert.eval(false);
  }

  public void testAggressive() throws Exception {
    setUp(new L1ReconnectConfigImpl(true, 5000, 5000, 1, 2));
    oooTest();
  }

  public void testLethargic() throws Exception {
    setUp(new L1ReconnectConfigImpl(true, 5000, 5000, 100, 1000));
    oooTest();
  }

  TCNetworkMessage    tcMessage = null;
  TestProtocolMessage pm        = null;
  final UUID          sessionId = UUID.getUUID();

  private void oooTest() throws InterruptedException {
    TestProtocolMessage msg;
    new PingMessage(new NullMessageMonitor());

    System.out.println("XXX 1 Client: " + clientGdp);
    System.out.println("XXX 1 Server: " + serverGdp);
    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().HANDSHAKE_WAIT_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().HANDSHAKE_WAIT_STATE);

    // case 1: Normal Handshake
    // server receives handshake request from a brand new client. Server replies to client with handshake ok and
    // notifies its GDP.
    msg = new TestProtocolMessage(null, 0, -1);
    msg.isHandshakeReplyOk = true;
    msg.setSessionId(sessionId);
    serverGdp.receive(msg);
    clientGdp.receive(msg);

    System.out.println("XXX 2 Client GDP: " + clientGdp);
    System.out.println("XXX 2 Server GDP: " + serverGdp + "\n\n");

    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);

    clientGdp.pause();
    clientGdp.reset();
    clientGdp.resume();

    serverGdp.pause();
    serverGdp.reset();
    serverGdp.resume();

    // case 2: Different session client connecting, server rejecting it.
    // Commented this, as GDP no more handles handshake Fail message
    // msg = new TestProtocolMessage(null, 0, -1);
    // msg.isHandshakeReplyFail = true;
    // serverGdp.receive(msg);
    // clientGdp.receive(msg);

    System.out.println("XXX 2 Client GDP: " + clientGdp);
    System.out.println("XXX 2 Server GDP: " + serverGdp);

    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().HANDSHAKE_WAIT_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().HANDSHAKE_WAIT_STATE);

    // Case 3: client sends messages to server and server acks it
    clientGdp.getSender().switchToState(clientGdp.getSender().MESSAGE_WAIT_STATE);
    serverGdp.getSender().switchToState(serverGdp.getSender().MESSAGE_WAIT_STATE);
    clientDelivery.clear();
    serverDelivery.clear();

    int sent = 0;
    while (sent < reconnectConfig.getMaxDelayAcks() - 1) {
      sendMessage(false);
      assertTrue(serverDelivery.ackCount == -1);
      serverDelivery.clear();
      Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE);
      Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);
      sent++;
    }

    sendMessage(true);
    assertTrue(serverDelivery.ackCount == sent);
    serverDelivery.clear();
    sent++;

    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);

    sendAckToClient();

    while (sent < reconnectConfig.getSendWindow() - 1) {
      if ((sent + 1) % (reconnectConfig.getMaxDelayAcks()) == 0) {
        sendMessage(true);
        assertTrue(serverDelivery.sentAck);
        assertTrue(serverDelivery.ackCount == sent);
        sendAckToClient();
      } else {
        sendMessage(false);
        assertFalse(serverDelivery.sentAck);
        assertTrue(serverDelivery.ackCount == (sent / (reconnectConfig.getMaxDelayAcks()))
                                              * (reconnectConfig.getMaxDelayAcks()) - 1);

      }

      Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE);
      Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);

      serverDelivery.clear();
      sent++;
    }

    sendMessage(true);
    assertTrue(serverDelivery.ackCount == sent);
    serverDelivery.clear();
    sent++;

    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);

    sendAckToClient();

    int toSend = sent + reconnectConfig.getSendWindow();
    while (sent < toSend) {
      System.out.println("XXX sending " + (sent + 1));
      sendMessage();
      sent++;
    }
    Assert.eval(clientGdp.getSender().getCurrentState() == clientGdp.getSender().SENDWINDOW_FULL_STATE);
    Assert.eval(serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE);

    sendMessageAndCheckQueued();
    sendMessageAndCheckQueued();

    System.out.println("XXX ACK to client");
    sendAckToClient();
    System.out.println("XXX Client GDP: " + clientGdp);
    System.out.println("XXX Server GDP: " + serverGdp);
    Assert.eval((clientGdp.getSender().getCurrentState() == clientGdp.getSender().MESSAGE_WAIT_STATE)
                || (clientGdp.getSender().getCurrentState() == clientGdp.getSender().SENDWINDOW_FULL_STATE));
    Assert.eval((serverGdp.getSender().getCurrentState() == serverGdp.getSender().MESSAGE_WAIT_STATE)
                || (serverGdp.getSender().getCurrentState() == serverGdp.getSender().SENDWINDOW_FULL_STATE));
  }

  private void sendMessage() throws InterruptedException {
    tcMessage = new PingMessage(new NullMessageMonitor());
    tcMessage.seal();
    assertTrue(clientDelivery.msg == null);
    clientGdp.send(tcMessage);
    assertTrue(clientDelivery.created);
    assertTrue(clientDelivery.msg != null);

    pm = (TestProtocolMessage) clientDelivery.msg;
    pm.setSessionId(sessionId);
    clientDelivery.clear();
    pm.isSend = true;
    serverGdp.receive(pm);
    assertTrue(serverReceiveQueue.take() == tcMessage);
  }

  private void sendMessage(boolean expectAckSentBack) throws InterruptedException {
    tcMessage = new PingMessage(new NullMessageMonitor());
    tcMessage.seal();
    assertTrue(clientDelivery.msg == null);
    clientGdp.send(tcMessage);
    assertTrue(clientDelivery.created);
    assertTrue(clientDelivery.msg != null);

    pm = (TestProtocolMessage) clientDelivery.msg;
    pm.setSessionId(sessionId);
    clientDelivery.clear();
    pm.isSend = true;
    serverGdp.receive(pm);
    assertTrue(serverReceiveQueue.take() == tcMessage);
    if (expectAckSentBack) {
      assertTrue(serverDelivery.sentAck);
    } else {
      assertFalse(serverDelivery.sentAck);
    }
  }

  private void sendMessageAndCheckQueued() {
    tcMessage = new PingMessage(new NullMessageMonitor());
    tcMessage.seal();
    assertTrue(clientDelivery.msg == null);
    clientGdp.send(tcMessage);
    assertFalse(clientDelivery.created);
    assertTrue(clientDelivery.msg == null);
    clientDelivery.clear();
  }

  private void sendAckToClient() {
    TestProtocolMessage ackMessage = new TestProtocolMessage();
    ackMessage.setSessionId(sessionId);
    ackMessage.ack = serverDelivery.ackCount;
    ackMessage.isAck = true;
    clientGdp.receive(ackMessage);
    clientDelivery.clear();
  }

  public void testTransportDisconnect() {
    setUp(new L1ReconnectConfigImpl());
    SendStateMachine sender = clientGdp.getSender();
    ReceiveStateMachine receiver = serverGdp.getReceiver();

    // both are handshaked
    TestProtocolMessage msg = new TestProtocolMessage();
    msg.isHandshakeReplyOk = true;
    msg.ack = -1;
    msg.setSessionId(sessionId);
    serverGdp.receive(msg);
    clientGdp.receive(msg);

    pm = new TestProtocolMessage(msg, 0, 0);
    pm.isSend = true;
    serverGdp.receive(pm);
    pm = new TestProtocolMessage(msg, 1, 1);
    pm.isSend = true;
    serverGdp.receive(pm);
    pm = new TestProtocolMessage(msg, 2, 2);
    pm.isSend = true;
    serverGdp.receive(pm);
    assertFalse(receiver.isClean());

    // simulate transport disconnected, call reset().
    clientGdp.reset();
    serverGdp.reset();
    assertTrue(sender.isClean());
    assertTrue(receiver.isClean());
  }
}
