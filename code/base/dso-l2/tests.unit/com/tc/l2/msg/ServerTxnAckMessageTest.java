/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.TestTransactionBatch;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.TestCommitTransactionMessage;
import com.tc.objectserver.tx.TestCommitTransactionMessageFactory;
import com.tc.objectserver.tx.TestServerTransaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class ServerTxnAckMessageTest extends TestCase {
  private AbstractGroupMessage relayedCommitTransactionMessage;
  private Set                  serverTransactionIDs;
  private final int            channelId = 2;
  private final NodeID         nodeID    = new ServerID("foo", "foobar".getBytes());

  @Override
  public void setUp() {
    TestCommitTransactionMessage testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage(GroupID.NULL_ID);
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
                                              .getInstance(false, 3452) }), new ObjectStringSerializerImpl());
    testCommitTransactionMessage.setChannelID(new ClientID(this.channelId));

    this.serverTransactionIDs = new HashSet();
    ClientID cid = new ClientID(this.channelId);
    ServerTransactionID stid1 = new ServerTransactionID(cid, new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(cid, new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(cid, new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(cid, new TransactionID(3460));
    this.serverTransactionIDs.add(stid1);
    this.serverTransactionIDs.add(stid2);
    this.serverTransactionIDs.add(stid3);
    this.serverTransactionIDs.add(stid4);

    List transactions = new ArrayList();
    transactions.add(new TestServerTransaction(stid1, new TxnBatchID(32), new GlobalTransactionID(23)));
    transactions.add(new TestServerTransaction(stid2, new TxnBatchID(12), new GlobalTransactionID(54)));
    transactions.add(new TestServerTransaction(stid3, new TxnBatchID(43), new GlobalTransactionID(55)));
    transactions.add(new TestServerTransaction(stid4, new TxnBatchID(9), new GlobalTransactionID(78)));

    this.relayedCommitTransactionMessage = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(testCommitTransactionMessage.getSourceNodeID(),
                                               testCommitTransactionMessage.getBatchData(), transactions, 700,
                                               new GlobalTransactionID(99),
                                               testCommitTransactionMessage.getSerializer());
    this.relayedCommitTransactionMessage.setMessageOrginator(this.nodeID);
  }

  @Override
  public void tearDown() {
    this.relayedCommitTransactionMessage = null;
    this.serverTransactionIDs = null;
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

    assertEquals(stam.getDestinationID(), this.nodeID);
  }

  private ServerTxnAckMessage writeAndRead(ServerTxnAckMessage stam) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    stam.serializeTo(bo);
    System.err.println("Written : " + stam);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    ServerTxnAckMessage stam1 = new ServerTxnAckMessage();
    stam1.deserializeFrom(bi);
    System.err.println("Read : " + stam1);
    return stam1;
  }

  public void testBasicSerialization() throws Exception {
    ServerTxnAckMessage stam = ServerTxnAckMessageFactory
        .createServerTxnAckMessage(this.relayedCommitTransactionMessage, this.serverTransactionIDs);
    ServerTxnAckMessage stam1 = writeAndRead(stam);
    validate(stam, stam1);
  }
}
