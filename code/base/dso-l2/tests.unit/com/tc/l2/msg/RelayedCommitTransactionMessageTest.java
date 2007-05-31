/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
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
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class RelayedCommitTransactionMessageTest extends TestCase {

  private TestCommitTransactionMessage testCommitTransactionMessage;
  private List                         transactions;
  private List                         serverTransactionIDs;
  private final int                    channelId = 2;

  public void setUp() {
    Collection acknowledged = new ArrayList();
    acknowledged.add(new TransactionID(987));
    acknowledged.add(new TransactionID(876));
    testCommitTransactionMessage = (TestCommitTransactionMessage) new TestCommitTransactionMessageFactory()
        .newCommitTransactionMessage();
    testCommitTransactionMessage.setBatch(new TestTransactionBatch(new TCByteBuffer[] { TCByteBufferFactory
        .getInstance(false, 3452) }, acknowledged), new ObjectStringSerializer());
    testCommitTransactionMessage.setChannelID(new ChannelID(channelId));

    serverTransactionIDs = new ArrayList();
    ServerTransactionID stid1 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(4234));
    ServerTransactionID stid2 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(6543));
    ServerTransactionID stid3 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(1654));
    ServerTransactionID stid4 = new ServerTransactionID(new ChannelID(channelId), new TransactionID(3460));
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

    assertEquals(rctm.getChannelID(), rctm1.getChannelID());

    Collection acknowledged = rctm.getAcknowledgedTransactionIDs();
    Collection acknowledged1 = rctm1.getAcknowledgedTransactionIDs();
    assertEquals(acknowledged.size(), acknowledged1.size());
    Iterator iter1 = acknowledged1.iterator();
    for (Iterator iter = acknowledged.iterator(); iter.hasNext();) {
      assertEquals(iter.next(), iter1.next());
    }

    for (Iterator iter = serverTransactionIDs.iterator(); iter.hasNext();) {
      ServerTransactionID serverTransactionID = (ServerTransactionID) iter.next();
      assertEquals(rctm.getOrCreateGlobalTransactionID(serverTransactionID), rctm1
          .getOrCreateGlobalTransactionID(serverTransactionID));
    }

    TCByteBuffer[] tcbb = rctm.getBatchData();
    TCByteBuffer[] tcbb1 = rctm1.getBatchData();
    assertEquals(tcbb.length, tcbb1.length);
    for (int i = 0; i < tcbb.length; i++) {
      assertEquals(tcbb[i].getBoolean(), tcbb1[i].getBoolean());
      assertEquals(tcbb[i].hasArray(), tcbb1[i].hasArray());
      assertEquals(tcbb[i].hasRemaining(), tcbb1[i].hasRemaining());
      assertEquals(tcbb[i].isDirect(), tcbb1[i].isDirect());
      assertEquals(tcbb[i].isReadOnly(), tcbb1[i].isReadOnly());
      byte[] byteArray = tcbb[i].array();
      byte[] byteArray1 = tcbb1[i].array();
      assertEquals(byteArray.length, byteArray1.length);
      for (int j = 0; j < byteArray.length; j++) {
        assertEquals(byteArray[j], byteArray1[j]);
      }
      assertEquals(tcbb[i].arrayOffset(), tcbb1[i].arrayOffset());
      assertEquals(tcbb[i].capacity(), tcbb1[i].capacity());
      assertEquals(tcbb[i].get(), tcbb1[i].get());
      assertEquals(tcbb[i].getChar(), tcbb1[i].getChar());
      assertEquals(tcbb[i].getInt(), tcbb1[i].getInt());
      assertEquals(tcbb[i].getLong(), tcbb1[i].getLong());
      assertEquals(tcbb[i].getShort(), tcbb1[i].getShort());
      assertEquals(tcbb[i].getUbyte(), tcbb1[i].getUbyte());
      assertEquals(tcbb[i].getUint(), tcbb1[i].getUint());
      assertEquals(tcbb[i].getUshort(), tcbb1[i].getUshort());
      assertEquals(tcbb[i].limit(), tcbb1[i].limit());
      assertEquals(tcbb[i].position(), tcbb1[i].position());
      assertEquals(tcbb[i].remaining(), tcbb1[i].remaining());

      assertEquals(rctm.getSequenceID(), rctm1.getSequenceID());
    }
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
        .createRelayedCommitTransactionMessage(testCommitTransactionMessage, transactions, 420);
    RelayedCommitTransactionMessage rctm1 = writeAndRead(rctm);
    validate(rctm, rctm1);
  }
}
