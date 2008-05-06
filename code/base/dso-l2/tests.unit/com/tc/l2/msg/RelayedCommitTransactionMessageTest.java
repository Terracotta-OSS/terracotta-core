/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCByteBufferTestUtil;
import com.tc.net.groups.ClientID;
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
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class RelayedCommitTransactionMessageTest extends TestCase {

  private TestCommitTransactionMessage testCommitTransactionMessage;
  private List                         transactions;
  private List                         serverTransactionIDs;
  private final int                    channelId = 2;

  public void setUp() {
    testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage();
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
        .getInstance(false, 3452) }), new ObjectStringSerializer());
    testCommitTransactionMessage.setChannelID(new ClientID(new ChannelID(channelId)));

    serverTransactionIDs = new ArrayList();
    ClientID cid = new ClientID(new ChannelID(channelId));
    ServerTransactionID stid1 = new ServerTransactionID(cid, new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(cid, new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(cid, new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(cid, new TransactionID(3460));
    serverTransactionIDs.add(stid1);
    serverTransactionIDs.add(stid2);
    serverTransactionIDs.add(stid3);
    serverTransactionIDs.add(stid4);

    transactions = new ArrayList();
    transactions.add(new TestServerTransaction(stid1, new TxnBatchID(32), new GlobalTransactionID(23)));
    transactions.add(new TestServerTransaction(stid2, new TxnBatchID(12), new GlobalTransactionID(54)));
    transactions.add(new TestServerTransaction(stid3, new TxnBatchID(43), new GlobalTransactionID(55)));
    transactions.add(new TestServerTransaction(stid4, new TxnBatchID(9), new GlobalTransactionID(78)));
  }

  public void tearDown() {
    testCommitTransactionMessage = null;
    transactions = null;
    serverTransactionIDs = null;
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

    for (Iterator iter = serverTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID serverTransactionID = (ServerTransactionID) iter.next();
      assertEquals(rctm.getOrCreateGlobalTransactionID(serverTransactionID), rctm1
          .getOrCreateGlobalTransactionID(serverTransactionID));
    }

    TCByteBuffer[] tcbb = rctm.getBatchData();
    TCByteBuffer[] tcbb1 = rctm1.getBatchData();
    TCByteBufferTestUtil.checkEquals(tcbb, tcbb1);
    assertEquals(rctm.getSequenceID(), rctm1.getSequenceID());
  }

  private RelayedCommitTransactionMessage writeAndRead(RelayedCommitTransactionMessage rctm) throws Exception {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    ObjectOutput oo = new ObjectOutputStream(bo);
    oo.writeObject(rctm);
    System.err.println("Written : " + rctm);
    ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
    ObjectInput oi = new ObjectInputStream(bi);
    RelayedCommitTransactionMessage rctm1 = (RelayedCommitTransactionMessage) oi.readObject();
    System.err.println("Read : " + rctm1);
    return rctm1;
  }

  public void testBasicSerialization() throws Exception {
    RelayedCommitTransactionMessage rctm = RelayedCommitTransactionMessageFactory
        .createRelayedCommitTransactionMessage(testCommitTransactionMessage, transactions, 420,
                                               new GlobalTransactionID(49));
    RelayedCommitTransactionMessage rctm1 = writeAndRead(rctm);
    validate(rctm, rctm1);
  }
}
