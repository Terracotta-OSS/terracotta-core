/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dmi.DmiClassSpec;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionBatchWriter;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;
import com.tc.object.tx.ClientTransactionImpl;
import com.tc.object.tx.TestClientTransaction;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionContextImpl;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;
import com.tc.util.SequenceGenerator;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

public class TransactionBatchTest extends TestCase {

  private final DNAEncodingInternal           encoding = new ApplicatorDNAEncodingImpl(new MockClassProvider());

  private ClientTransactionBatchWriter        writer;
  private TestCommitTransactionMessageFactory messageFactory;

  @Override
  public void setUp() throws Exception {
    this.messageFactory = new TestCommitTransactionMessageFactory();
    this.writer = newWriter(new ObjectStringSerializerImpl());
  }

  private ClientTransactionBatchWriter newWriter(final ObjectStringSerializer serializer) {
    return new ClientTransactionBatchWriter(GroupID.NULL_ID, new TxnBatchID(1), serializer, this.encoding,
                                            this.messageFactory, FoldingConfig.createFromProperties(TCPropertiesImpl
                                                .getProperties()));
  }

  private ClientTransactionBatchWriter newWriter(final ObjectStringSerializer serializer, final boolean foldEnabled,
                                                 final int lockLimit, final int objectLimit) {
    return new ClientTransactionBatchWriter(GroupID.NULL_ID, new TxnBatchID(1), serializer, this.encoding,
                                            this.messageFactory, new FoldingConfig(foldEnabled, objectLimit, lockLimit));
  }

  public void testGetMinTransaction() throws Exception {
    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();

    final LinkedList list = new LinkedList();
    for (int i = 0; i < 100; i++) {
      final TestClientTransaction tx = new TestClientTransaction();
      tx.txID = new TransactionID(i);
      tx.txnType = TxnType.NORMAL;
      tx.allLockIDs = Arrays.asList(new Object[] { new StringLockID("" + i) });
      list.add(tx);
      FoldedInfo fi = writer.addTransaction(tx, sequenceGenerator, tidGenerator);
      Assert.assertFalse(fi.isFolded());
    }

    assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), this.writer.getMinTransactionSequence());

    // remove some from the middle and make sure the min is constant
    for (int i = 50; i < 55; i++) {
      final ClientTransaction tx = (ClientTransaction) list.remove(i);
      this.writer.removeTransaction(tx.getTransactionID());
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), this.writer.getMinTransactionSequence());
    }

    // now remove the least transaction and make sure the min increases.
    for (final Iterator i = list.iterator(); i.hasNext();) {
      final ClientTransaction tx = (ClientTransaction) i.next();
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), this.writer.getMinTransactionSequence());
      i.remove();
      this.writer.removeTransaction(tx.getTransactionID());
    }
  }

  public void testSend() throws Exception {
    assertTrue(this.messageFactory.messages.isEmpty());

    this.writer.send();
    assertEquals(1, this.messageFactory.messages.size());
    final TestCommitTransactionMessage message = (TestCommitTransactionMessage) this.messageFactory.messages.get(0);
    assertEquals(1, message.setBatchCalls.size());
    assertSame(this.writer, message.setBatchCalls.get(0));
    assertEquals(1, message.sendCalls.size());
  }

  public void testWriteRead() throws IOException {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    final TestCommitTransactionMessageFactory mf = new TestCommitTransactionMessageFactory();
    final ClientID clientID = new ClientID(69);
    final TxnBatchID batchID = new TxnBatchID(42);

    final List tx1Notifies = new LinkedList();
    // A nested transaction (all this buys us is more than 1 lock in a txn)
    final LockID lid1 = new StringLockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction tmp = new ClientTransactionImpl(new NullRuntimeLogger());
    tmp.setTransactionContext(tc);
    final LockID lid2 = new StringLockID("2");
    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL, Arrays.asList(new LockID[] { lid1, lid2 }));
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);

    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final MockTCObject mtco = new MockTCObject(new ObjectID(2), this);
    mtco.setNew(true);

    txn1.createObject(mtco);
    txn1.createRoot("root", new ObjectID(3));
    for (int i = 0; i < 10; i++) {
      final Notify notify = new NotifyImpl(new StringLockID("" + i), new ThreadID(i), i % 2 == 0);
      tx1Notifies.add(notify);
      txn1.addNotify(notify);
    }

    tc = new TransactionContextImpl(new StringLockID("3"), TxnType.CONCURRENT, TxnType.CONCURRENT);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);

    this.writer = new ClientTransactionBatchWriter(GroupID.NULL_ID, batchID, serializer, this.encoding, mf,
                                                   FoldingConfig.createFromProperties(TCPropertiesImpl.getProperties()));

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();

    this.writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    this.writer.addTransaction(txn2, sequenceGenerator, tidGenerator);

    final DSOGlobalServerStats stats = getDSOGlobalServerStats();
    final TransactionBatchReaderImpl reader = new TransactionBatchReaderImpl(this.writer.getData(), clientID,
                                                                             serializer,
                                                                             new ActiveServerTransactionFactory(),
                                                                             stats);
    // let transactionSize counter sample
    ThreadUtil.reallySleep(2000);
    assertTransactionSize(this.writer.getData(), 2, stats.getTransactionSizeCounter());

    assertEquals(2, reader.getNumberForTxns());
    assertEquals(batchID, reader.getBatchID());

    int count = 0;
    ServerTransaction txn;
    while ((txn = reader.getNextTransaction()) != null) {
      count++;
      assertEquals(clientID, txn.getSourceID());
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
          assertTrue(Arrays.equals(new LockID[] { new StringLockID("1"), new StringLockID("2") }, txn.getLockIDs()));
          assertEquals(tx1Notifies, txn.getNotifies());
          break;
        case 2:
          assertEquals(0, txn.getChanges().size());
          assertEquals(0, txn.getNewRoots().size());
          assertEquals(0, txn.getObjectIDs().size());
          assertEquals(TxnType.CONCURRENT, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new StringLockID("3") }, txn.getLockIDs()));
          break;
        default:
          fail("count is " + count);
      }
    }

    assertEquals(2, count);

  }

  private DSOGlobalServerStats getDSOGlobalServerStats() {
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledRateCounter transactionSizeCounter = (SampledRateCounter) counterManager
        .createCounter(new SampledRateCounterConfig(1, 10, false));
    final DSOGlobalServerStats stats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null,
                                                                    null, null, transactionSizeCounter, null);
    return stats;
  }

  public void testSimpleFold() throws IOException {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();

    this.writer = newWriter(serializer, true, 0, 0);

    final ClientID clientID = new ClientID(69);

    final LockID lid1 = new StringLockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    // txn3 has more objects than 1 & 2, but contains all from the previous, it can be folded
    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn3.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    // this txn has a common object although not share a common lock with the others -- it should be folded
    final LockID lid2 = new StringLockID("2");

    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn4 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn4.setTransactionContext(tc);
    txn4.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    fi = writer.addTransaction(txn4, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    final DSOGlobalServerStats stats = getDSOGlobalServerStats();
    final TransactionBatchReaderImpl reader = new TransactionBatchReaderImpl(this.writer.getData(), clientID,
                                                                             serializer,
                                                                             new ActiveServerTransactionFactory(),
                                                                             stats);
    // let transactionSize counter sample
    ThreadUtil.reallySleep(2000);
    assertTransactionSize(writer.getData(), 1, stats.getTransactionSizeCounter());

    assertEquals(1, reader.getNumberForTxns());
    assertEquals(new TxnBatchID(1), reader.getBatchID());

    int count = 0;
    ServerTransaction txn;
    while ((txn = reader.getNextTransaction()) != null) {
      count++;
      assertEquals(clientID, txn.getSourceID());

      switch (count) {
        case 1:
          assertEquals(1, txn.getTransactionID().toLong());
          assertEquals(2, txn.getChanges().size());
          assertEquals(4, txn.getNumApplicationTxn());
          assertEquals(0, txn.getNewRoots().size());
          assertEquals(2, txn.getObjectIDs().size());
          assertTrue(txn.getObjectIDs().containsAll(Arrays.asList(new ObjectID[] { new ObjectID(1), new ObjectID(2) })));
          assertEquals(TxnType.NORMAL, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new StringLockID("1") }, txn.getLockIDs()));
          assertEquals(Collections.EMPTY_LIST, txn.getNotifies());
          break;
        case 2:
          assertEquals(2, txn.getTransactionID().toLong());
          assertEquals(1, txn.getNumApplicationTxn());
          assertEquals(1, txn.getChanges().size());
          assertEquals(0, txn.getNewRoots().size());
          assertEquals(1, txn.getObjectIDs().size());
          assertTrue(txn.getObjectIDs().containsAll(Arrays.asList(new ObjectID[] { new ObjectID(2) })));
          assertEquals(TxnType.NORMAL, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new StringLockID("2") }, txn.getLockIDs()));
          assertEquals(Collections.EMPTY_LIST, txn.getNotifies());
          break;
        default:
          fail("count is " + count);
      }
    }
  }

  private void assertTransactionSize(TCByteBuffer[] actualData, int actualNumTxns,
                                     SampledRateCounter transactionSizeCounter) {
    long expectedAvgTxnSize = new TCByteBufferInputStream(actualData).getTotalLength() / actualNumTxns;
    long actualAvgTxnSize = transactionSizeCounter.getMostRecentSample().getCounterValue();

    assertEquals(expectedAvgTxnSize, actualAvgTxnSize);
  }

  public void testFoldObjectLimit() {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 0, 2);

    final LockID lid1 = new StringLockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn1.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn1.fieldChanged(new MockTCObject(new ObjectID(3), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn2.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn2.fieldChanged(new MockTCObject(new ObjectID(3), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    // txn1 and txn2 exceed the object limit (should not fold)
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldLockLimit() {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 2, 0);

    final LockID lid1 = new StringLockID("1");
    final LockID lid2 = new StringLockID("2");
    final LockID lid3 = new StringLockID("3");
    final List threeLocks = Arrays.asList(lid1, lid2, lid3);

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL, threeLocks);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL, threeLocks);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    // txn1 and txn2 exceed the lock limit (should not fold)
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldDisabled() {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, false, 0, 0);

    final LockID lid1 = new StringLockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    // folding disabled (these txns would normally fold)
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testDisallowedFolds() {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 0, 0);

    final LockID lid1 = new StringLockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txnWithRoot = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithRoot.setTransactionContext(tc);
    txnWithRoot.createRoot("root", new ObjectID(234));

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txnWithDMI = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithDMI.setTransactionContext(tc);
    txnWithDMI.addDmiDescriptor(new DmiDescriptor(new ObjectID(12), new ObjectID(13), new DmiClassSpec[] {}, true));

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txnWithNotify = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithNotify.setTransactionContext(tc);
    txnWithNotify.addNotify(new NotifyImpl(lid1, new ThreadID(122), true));

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    // Txns with DMI, root or notifies do not qualify for folds
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txnWithRoot, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txnWithDMI, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(3 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txnWithNotify, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(4 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldBug1() {
    // Consider these 3 txns...
    // (txn1) - Lock1, Obj1(delta)
    // (txn2) - Lock2, Obj2(new)
    // (txn3) - Lock1, Obj1(delta), Obj2(delta)
    //
    // txn3 cannot be folded into txn1 because it would put the Obj2 delta before the txn that creates it (txn2)

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 0, 0);

    final LockID lid1 = new StringLockID("1");
    final LockID lid2 = new StringLockID("2");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    final MockTCObject mtco = new MockTCObject(new ObjectID(2), new Object());
    mtco.setNew(true);
    txn2.createObject(mtco);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn3.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
  }

  public void testTxnWithNewObjCanBeFolded() {
    // (txn1) - Lock1, Obj1(delta)
    // (txn2) - Lock1, Obj1(delta), Obj2(new)
    //
    // txn2 should be folded into txn1 even though it contains a "new" object

    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 0, 0);

    final LockID lid1 = new StringLockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    final MockTCObject mtco = new MockTCObject(new ObjectID(2), new Object());
    mtco.setNew(true);
    txn2.createObject(mtco);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testOrdering() {
    final ObjectStringSerializer serializer = new ObjectStringSerializerImpl();
    this.writer = newWriter(serializer, true, 0, 0);

    final LockID lid1 = new StringLockID("1");
    final LockID lid2 = new StringLockID("2");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    final ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    final SequenceGenerator sequenceGenerator = new SequenceGenerator();
    final TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    FoldedInfo fi;

    // There is a common object between txn1 and txn2 (but differing locks). This should fold
    fi = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    fi = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    fi = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertTrue(fi.isFolded());
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  static class BatchWriterProperties implements TCProperties {
    private final int     objectLimit;
    private final int     lockLimit;
    private final boolean foldEnabled;

    BatchWriterProperties(final boolean foldEnabled, final int lockLimit, final int objectLimit) {
      this.foldEnabled = foldEnabled;
      this.lockLimit = lockLimit;
      this.objectLimit = objectLimit;
    }

    public Properties addAllPropertiesTo(final Properties properties) {
      throw new AssertionError();
    }

    public boolean getBoolean(final String key, final boolean defaultValue) {
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_ENABLED.equals(key)) { return this.foldEnabled; }

      throw new AssertionError("key: " + key);
    }

    public boolean getBoolean(final String key) {
      throw new AssertionError();
    }

    public float getFloat(final String key) {
      throw new AssertionError();
    }

    public int getInt(final String key, final int defaultValue) {
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT.equals(key)) { return this.lockLimit; }
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT.equals(key)) { return this.objectLimit; }
      throw new AssertionError("key: " + key);
    }

    public int getInt(final String key) {
      throw new AssertionError();
    }

    public long getLong(final String key) {
      throw new AssertionError();
    }

    public TCProperties getPropertiesFor(final String key) {
      throw new AssertionError();
    }

    public String getProperty(final String key, final boolean missingOkay) {
      throw new AssertionError();
    }

    public String getProperty(final String key) {
      throw new AssertionError();
    }

    public long getLong(final String key, final long defaultValue) {
      throw new AssertionError();
    }

    public void overwriteTcPropertiesFromConfig(Map<String, String> props) {
      throw new AssertionError();
    }

    public void setProperty(final String key, final String value) {
      throw new AssertionError();
    }

  }

}
