/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCByteBufferTestUtil;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
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
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class RelayedCommitTransactionMessageTest extends TestCase {

  private TestCommitTransactionMessage testCommitTransactionMessage;
  private List                         transactions;
  private List                         serverTransactionIDs;
  private final ClientID               cid = new ClientID(2);

  @Override
  public void setUp() {
    this.testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage(GroupID.NULL_ID);
    this.testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
                                                   .getInstance(false, 3452) }), new ObjectStringSerializerImpl());
    this.testCommitTransactionMessage.setChannelID(this.cid);

    this.serverTransactionIDs = new ArrayList();
    ServerTransactionID stid1 = new ServerTransactionID(this.cid, new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(this.cid, new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(this.cid, new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(this.cid, new TransactionID(3460));
    this.serverTransactionIDs.add(stid1);
    this.serverTransactionIDs.add(stid2);
    this.serverTransactionIDs.add(stid3);
    this.serverTransactionIDs.add(stid4);

    this.transactions = new ArrayList();
    this.transactions.add(new TestServerTransaction(stid1, new TxnBatchID(32), new GlobalTransactionID(23)));
    this.transactions.add(new TestServerTransaction(stid2, new TxnBatchID(12), new GlobalTransactionID(54)));
    this.transactions.add(new TestServerTransaction(stid3, new TxnBatchID(43), new GlobalTransactionID(55)));
    this.transactions.add(new TestServerTransaction(stid4, new TxnBatchID(9), new GlobalTransactionID(78)));
  }

  @Override
  public void tearDown() {
    this.testCommitTransactionMessage = null;
    this.transactions = null;
    this.serverTransactionIDs = null;
  }

  private void validate(RelayedCommitTransactionMessage rctm, RelayedCommitTransactionMessage rctm1) {
    assertEquals(rctm.getType(), rctm1.getType());
    assertEquals(rctm.getMessageID(), rctm1.getMessageID());
    assertEquals(rctm.inResponseTo(), rctm1.inResponseTo());
    assertEquals(rctm.messageFrom(), rctm1.messageFrom());

    assertEquals(rctm.getClientID(), rctm1.getClientID());

    GlobalTransactionID lwm = rctm.getLowGlobalTransactionIDWatermark();
    GlobalTransactionID lwm1 = rctm1.getLowGlobalTransactionIDWatermark();
    assertEquals(lwm, lwm1);

    for (Iterator iter = this.serverTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID serverTransactionID = (ServerTransactionID) iter.next();
      assertEquals(rctm.getGlobalTransactionIDFor(serverTransactionID),
                   rctm1.getGlobalTransactionIDFor(serverTransactionID));
    }

    TCByteBuffer[] tcbb = rctm.getBatchData();
    TCByteBuffer[] tcbb1 = rctm1.getBatchData();
    TCByteBufferTestUtil.checkEquals(tcbb, tcbb1);
    assertEquals(rctm.getSequenceID(), rctm1.getSequenceID());
  }

  private RelayedCommitTransactionMessage writeAndRead(RelayedCommitTransactionMessage rctm) throws Exception {
    TCByteBufferOutputStream bo = new TCByteBufferOutputStream();
    rctm.serializeTo(bo);
    System.err.println("Written : " + rctm);
    TCByteBufferInputStream bi = new TCByteBufferInputStream(bo.toArray());
    RelayedCommitTransactionMessage rctm1 = new RelayedCommitTransactionMessage();
    rctm1.deserializeFrom(bi);
    System.err.println("Read : " + rctm1);
    return rctm1;
  }

  public void testBasicSerialization() throws Exception {
    RelayedCommitTransactionMessage rctm = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(this.cid, this.testCommitTransactionMessage.getBatchData(),
                                               this.transactions, 420, new GlobalTransactionID(49),
                                               this.testCommitTransactionMessage.getSerializer());
    RelayedCommitTransactionMessage rctm1 = writeAndRead(rctm);
    validate(rctm, rctm1);
  }
}
