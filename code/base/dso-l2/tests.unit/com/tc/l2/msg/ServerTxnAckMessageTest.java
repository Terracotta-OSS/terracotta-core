/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.TestTransactionBatch;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.TestCommitTransactionMessage;
import com.tc.objectserver.tx.TestCommitTransactionMessageFactory;
import com.tc.objectserver.tx.TestServerTransaction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class ServerTxnAckMessageTest extends TestCase {
  private AbstractGroupMessage relayedCommitTransactionMessage;
  private Set                  serverTransactionIDs;
  private final int            channelId = 2;
  private final NodeID nodeID = new NodeID("foo", "foobar".getBytes());

  public void setUp() {
    Collection acknowledged = new ArrayList();
    acknowledged.add(new TransactionID(987));
    acknowledged.add(new TransactionID(876));
    TestCommitTransactionMessage testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage();
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
        .getInstance(false, 3452) }, acknowledged), new ObjectStringSerializer());
    testCommitTransactionMessage.setChannelID(new ChannelID(channelId));

    serverTransactionIDs = new HashSet();
    ServerTransactionID stid1 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(3460));
    serverTransactionIDs.add(stid1);
    serverTransactionIDs.add(stid2);
    serverTransactionIDs.add(stid3);
    serverTransactionIDs.add(stid4);

    List transactions = new ArrayList();
    transactions.add(new TestServerTransaction(stid1, new TxnBatchID(32), new GlobalTransactionID(23)));
    transactions.add(new TestServerTransaction(stid2, new TxnBatchID(12), new GlobalTransactionID(54)));
    transactions.add(new TestServerTransaction(stid3, new TxnBatchID(43), new GlobalTransactionID(55)));
    transactions.add(new TestServerTransaction(stid4, new TxnBatchID(9), new GlobalTransactionID(78)));

    relayedCommitTransactionMessage = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(testCommitTransactionMessage, transactions);
    relayedCommitTransactionMessage.setMessageOrginator(nodeID);
  }

  public void tearDown() {
    relayedCommitTransactionMessage = null;
    serverTransactionIDs = null;
  }

  private void validate(ServerTxnAckMessage stam, ServerTxnAckMessage stam1) {
    assertEquals(stam.getType(), stam1.getType());
    assertEquals(stam.getMessageID(), stam1.getMessageID());
    assertEquals(stam.inResponseTo(), stam1.inResponseTo());
    assertEquals(stam.messageFrom(), stam1.messageFrom());

    Set acked = stam.getAckedServerTxnIDs();
    Set acked1 = stam1.getAckedServerTxnIDs();
    assertEquals(acked.size(), acked1.size());
    for (Iterator iter = acked.iterator(); iter.hasNext();) {
      ServerTransactionID stid = (ServerTransactionID) iter.next();
      assertTrue(acked1.contains(stid));
      acked1.remove(stid);
    }
    assertTrue(acked1.isEmpty());
    
    assertEquals(stam.getDestinationID(), nodeID);
  }

  private ServerTxnAckMessage writeAndRead(ServerTxnAckMessage stam) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(stam);
    System.err.println("Written : " + stam);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    ServerTxnAckMessage stam1 = (ServerTxnAckMessage) oi.readObject();
    System.err.println("Read : " + stam1);
    return stam1;
  }

  public void testBasicSerialization() throws Exception {
    ServerTxnAckMessage stam = ServerTxnAckMessageFactory.createServerTxnAckMessage(relayedCommitTransactionMessage,
                                                                                    serverTransactionIDs);
    ServerTxnAckMessage stam1 = writeAndRead(stam);
    validate(stam, stam1);
  }
}
