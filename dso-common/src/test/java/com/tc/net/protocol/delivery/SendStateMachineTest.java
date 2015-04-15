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

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.test.TCTestCase;
import com.tc.util.UUID;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Testing the basic functionality of OOO Send State Machine. More functional test at GuaranteedDeliveryProtocolTest
 */
public class SendStateMachineTest extends TCTestCase {

  public void tests() throws Exception {
    TestProtocolMessageDelivery delivery = new TestProtocolMessageDelivery(new LinkedBlockingQueue<TCNetworkMessage>());
    final UUID sessionId = UUID.getUUID();
    final ReconnectConfig reconnectConfig = new L1ReconnectConfigImpl(true, 5000, 100, 16, 32);
    SendStateMachine ssm = new SendStateMachine(delivery, reconnectConfig, true);
    ssm.start();
    ssm.resume();

    // hand shake state
    // compelete hand shake, receive ack=-1 from receiver
    TestProtocolMessage msg = new TestProtocolMessage(null, 0, -1);
    msg.isHandshakeReplyOk = true;
    msg.setSessionId(sessionId);
    ssm.execute(msg);

    // SEND
    MessageMonitor monitor = new NullMessageMonitor();
    ssm.put(new PingMessage(monitor));
    ssm.execute(null); // msg 0
    assertTrue(delivery.created);
    assertTrue(delivery.msg.getSent() == 0);
    delivery.clearAll();

    TestProtocolMessage tpm = new TestProtocolMessage(null, -1, -1);
    tpm.setSessionId(sessionId);
    tpm.isSend = true;

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
    ssm.put(new PingMessage(monitor));
    ssm.execute(tpm);
    assertEquals(2, delivery.msg.getSent());

    // HAND SHAKE for RESEND
    delivery.clearAll();
    ssm.resume();
    assertFalse(ssm.isPaused());

    msg = new TestProtocolMessage(null, 0, -1);
    msg.isHandshakeReplyOk = true;
    msg.setSessionId(sessionId);
    ssm.execute(msg);

    tpm.ack = 0;
    ssm.execute(tpm); // dup ack=0

    ssm.put(new PingMessage(monitor)); // msg 4
    ssm.execute(null);
    assertEquals(4, delivery.msg.getSent());

    tpm.ack = 2;
    ssm.execute(tpm); // ack 2
    assertEquals(4, delivery.msg.getSent());

  }
}
