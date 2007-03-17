/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionImpl;
import com.tc.object.tx.TestClientTransaction;
import com.tc.object.tx.TransactionBatch;
import com.tc.object.tx.TransactionBatchWriter;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class TransactionBatchTest extends TestCase {

  private DNAEncoding                         encoding = new DNAEncoding(new MockClassProvider());

  private TransactionBatchWriter              writer;
  private TestCommitTransactionMessageFactory messageFactory;

  public void setUp() throws Exception {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    messageFactory = new TestCommitTransactionMessageFactory();
    writer = new TransactionBatchWriter(new TxnBatchID(1), serializer, encoding, messageFactory);
  }

  public void testGetMinTransaction() throws Exception {
    LinkedList list = new LinkedList();
    for (int i = 0; i < 100; i++) {
      TestClientTransaction tx = new TestClientTransaction();
      tx.sequenceID = new SequenceID(i);
      tx.txID = new TransactionID(i);
      tx.txnType = TxnType.NORMAL;
      list.add(tx);
      writer.addTransaction(tx);
    }

    writer.wait4AllTxns2Serialize();

    assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());

    // remove some from the middle and make sure the min is constant
    for (int i = 50; i < 55; i++) {
      ClientTransaction tx = (ClientTransaction) list.remove(i);
      writer.removeTransaction(tx.getTransactionID());
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());
    }

    // now remove the leastmost transaction and make sure the min increases.
    for (Iterator i = list.iterator(); i.hasNext();) {
      ClientTransaction tx = (ClientTransaction) i.next();
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());
      i.remove();
      writer.removeTransaction(tx.getTransactionID());
    }
  }

  public void testAddAcknowledgedTransactionIDs() throws Exception {
    Set txs = new HashSet();
    for (int i = 0; i < 100; i++) {
      GlobalTransactionID txID = new GlobalTransactionID(i);
      txs.add(txID);
    }
    writer.addAcknowledgedTransactionIDs(txs);
    assertEquals(txs, writer.getAcknowledgedTransactionIDs());
  }

  public void testSend() throws Exception {
    assertTrue(messageFactory.messages.isEmpty());

    writer.send();

    assertEquals(1, messageFactory.messages.size());
    TestCommitTransactionMessage message = (TestCommitTransactionMessage) messageFactory.messages.get(0);
    assertEquals(1, message.setBatchCalls.size());
    assertSame(writer, message.setBatchCalls.get(0));
    assertEquals(1, message.sendCalls.size());
  }

  public void testWriteRead() throws IOException {
    long sequence = 0;
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    TestCommitTransactionMessageFactory mf = new TestCommitTransactionMessageFactory();
    ChannelID channel = new ChannelID(69);
    TxnBatchID batchID = new TxnBatchID(42);

    List tx1Notifies = new LinkedList();
    // A nested transaction (all this buys us is more than 1 lock in a txn)
    LockID lid1 = new LockID("1");
    TransactionContext tc = new TransactionContext(lid1, TxnType.NORMAL, new LockID[] { lid1 });
    ClientTransaction tmp = new ClientTransactionImpl(new TransactionID(101), new NullRuntimeLogger(), null);
    tmp.setTransactionContext(tc);
    LockID lid2 = new LockID("2");
    tc = new TransactionContext(lid2, TxnType.NORMAL, new LockID[] { lid1, lid2 });
    ClientTransaction txn1 = new ClientTransactionImpl(new TransactionID(1), new NullRuntimeLogger(), null);
    txn1.setTransactionContext(tc);

    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn1.createObject(new MockTCObject(new ObjectID(2), this));
    txn1.createRoot("root", new ObjectID(3));
    for (int i = 0; i < 10; i++) {
      Notify notify = new Notify(new LockID("" + i), new ThreadID(i), i % 2 == 0);
      tx1Notifies.add(notify);
      txn1.addNotify(notify);
    }

    tc = new TransactionContext(new LockID("3"), TxnType.CONCURRENT, new LockID[] { new LockID("3") });
    ClientTransaction txn2 = new ClientTransactionImpl(new TransactionID(2), new NullRuntimeLogger(), null);
    txn2.setTransactionContext(tc);

    writer = new TransactionBatchWriter(batchID, serializer, encoding, mf);

    txn1.setSequenceID(new SequenceID(++sequence));
    txn2.setSequenceID(new SequenceID(++sequence));
    writer.addTransaction(txn1);
    writer.addTransaction(txn2);
    writer.wait4AllTxns2Serialize();

    TransactionBatchReaderImpl reader = new TransactionBatchReaderImpl(writer.getData(), channel, new HashSet(),
                                                                       serializer, false);
    assertEquals(2, reader.getNumTxns());
    assertEquals(batchID, reader.getBatchID());

    int count = 0;
    ServerTransaction txn;
    while ((txn = reader.getNextTransaction()) != null) {
      count++;
      assertEquals(channel, txn.getChannelID());
      assertEquals(count, txn.getTransactionID().toLong());

      switch (count) {
        case 1:
          assertEquals(2, txn.getChanges().size());
          assertEquals(1, txn.getNewRoots().size());
          assertEquals("root", txn.getNewRoots().keySet().toArray()[0]);
          assertEquals(new ObjectID(3), txn.getNewRoots().values().toArray()[0]);
          assertEquals(2, txn.getObjectIDs().size());
          assertTrue(txn.getObjectIDs().containsAll(Arrays.asList(new ObjectID[] { new ObjectID(1), new ObjectID(2) })));
          assertEquals(TxnType.NORMAL, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new LockID("1"), new LockID("2") }, txn.getLockIDs()));
          assertEquals(tx1Notifies, txn.addNotifiesTo(new LinkedList()));
          break;
        case 2:
          assertEquals(0, txn.getChanges().size());
          assertEquals(0, txn.getNewRoots().size());
          assertEquals(0, txn.getObjectIDs().size());
          assertEquals(TxnType.CONCURRENT, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new LockID("3") }, txn.getLockIDs()));
          break;
        default:
          fail("count is " + count);
      }
    }

    assertEquals(2, count);

  }

  private static final class TestCommitTransactionMessageFactory implements CommitTransactionMessageFactory {

    public final List messages = new LinkedList();

    public CommitTransactionMessage newCommitTransactionMessage() {
      CommitTransactionMessage rv = new TestCommitTransactionMessage();
      messages.add(rv);
      return rv;
    }

  }

  private static final class TestCommitTransactionMessage implements CommitTransactionMessage {

    public final List             setBatchCalls = new LinkedList();
    public final List             sendCalls     = new LinkedList();
    public ObjectStringSerializer serializer;

    public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer) {
      setBatchCalls.add(batch);
      this.serializer = serializer;
    }

    public TCByteBuffer[] getBatchData() {
      return null;
    }

    public void send() {
      this.sendCalls.add(new Object());
    }

    public ObjectStringSerializer getSerializer() {
      return serializer;
    }

    public Collection addAcknowledgedTransactionIDsTo(Collection c) {
      throw new ImplementMe();
    }

    public ChannelID getChannelID() {
      return ChannelID.NULL_ID;
    }

  }

}
