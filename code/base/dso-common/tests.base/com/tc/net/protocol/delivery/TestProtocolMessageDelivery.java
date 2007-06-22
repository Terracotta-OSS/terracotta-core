/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.delivery.OOOProtocolMessage;
import com.tc.net.protocol.delivery.OOOProtocolMessageDelivery;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.util.Assert;

/**
 *
 */
public class TestProtocolMessageDelivery implements OOOProtocolMessageDelivery {
  public boolean            sentAckRequest = false;
  public long               ackCount       = -1;
  public boolean            sentAck        = false;
  public OOOProtocolMessage msg            = null;
  public TCNetworkMessage   tcMessage      = null;
  public boolean            created        = false;
  public int                receivedMessageCount;
  private LinkedQueue       receivedQueue;

  public TestProtocolMessageDelivery(LinkedQueue receivedQueue) {
    this.receivedQueue = receivedQueue;
  }

  public OOOProtocolMessage createAckRequestMessage(short sessionId) {
    sentAckRequest = true;
    return (new TestProtocolMessage());
  }

  public void sendAckRequest() {
    sentAckRequest = true;
  }

  public OOOProtocolMessage createAckMessage(long sequence) {
    this.ackCount = sequence;
    this.sentAck = true;

    TestProtocolMessage opm = new TestProtocolMessage(null, 0, sequence);
    opm.isAck = true;
    return (opm);
  }

  public void sendAck(long count) {
    this.ackCount = count;
    this.sentAck = true;
  }

  public void sendMessage(OOOProtocolMessage pmsg) {
    this.msg = pmsg;
  }

  public OOOProtocolMessage createProtocolMessage(long sent, short sessionId, TCNetworkMessage tcmsg) {
    Assert.eval(sent >= 0);
    this.tcMessage = tcmsg;
    this.created = true;
    TestProtocolMessage tpm = new TestProtocolMessage(tcmsg, sent, 0);
    tpm.isSend = true;
    return tpm;
  }

  public void clear() {
    sentAck = false;
    sentAckRequest = false;
    ackCount = -1;
    msg = null;
    tcMessage = null;
    created = false;
  }

  public void receiveMessage(OOOProtocolMessage pm) {
    receivedMessageCount++;
    try {
      receivedQueue.put(((TestProtocolMessage) pm).msg);
    } catch (InterruptedException e) {
      e.printStackTrace();
      junit.framework.Assert.fail("yikes! " + e);
    }

  }

  public ConnectionID getConnectionId() {
    return null;
  }

}