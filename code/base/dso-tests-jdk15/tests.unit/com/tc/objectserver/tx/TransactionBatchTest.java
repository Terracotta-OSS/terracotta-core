/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.config.TcProperty;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ApplicatorDNAEncodingImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dmi.DmiClassSpec;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionImpl;
import com.tc.object.tx.TestClientTransaction;
import com.tc.object.tx.TransactionBatchWriter;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionContextImpl;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;
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
import java.util.Properties;

import junit.framework.TestCase;

public class TransactionBatchTest extends TestCase {

  private DNAEncoding                         encoding = new ApplicatorDNAEncodingImpl(new MockClassProvider());

  private TransactionBatchWriter              writer;
  private TestCommitTransactionMessageFactory messageFactory;

  public void setUp() throws Exception {
    messageFactory = new TestCommitTransactionMessageFactory();
    writer = newWriter(new ObjectStringSerializer());
  }

  private TransactionBatchWriter newWriter(ObjectStringSerializer serializer) {
    return new TransactionBatchWriter(GroupID.NULL_ID, new TxnBatchID(1), serializer, encoding, messageFactory,
                                      FoldingConfig.createFromProperties(TCPropertiesImpl.getProperties()));
  }

  private TransactionBatchWriter newWriter(ObjectStringSerializer serializer, boolean foldEnabled, int lockLimit,
                                           int objectLimit) {
    return new TransactionBatchWriter(GroupID.NULL_ID, new TxnBatchID(1), serializer, encoding, messageFactory,
                                      new FoldingConfig(foldEnabled, objectLimit, lockLimit));
  }

  public void testGetMinTransaction() throws Exception {
    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();

    LinkedList list = new LinkedList();
    for (int i = 0; i < 100; i++) {
      TestClientTransaction tx = new TestClientTransaction();
      tx.txID = new TransactionID(i);
      tx.txnType = TxnType.NORMAL;
      tx.allLockIDs = Arrays.asList(new Object[] { new LockID("" + i) });
      list.add(tx);
      boolean folded = writer.addTransaction(tx, sequenceGenerator, tidGenerator);
      Assert.assertFalse(folded);
    }

    assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());

    // remove some from the middle and make sure the min is constant
    for (int i = 50; i < 55; i++) {
      ClientTransaction tx = (ClientTransaction) list.remove(i);
      writer.removeTransaction(tx.getTransactionID());
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());
    }

    // now remove the least transaction and make sure the min increases.
    for (Iterator i = list.iterator(); i.hasNext();) {
      ClientTransaction tx = (ClientTransaction) i.next();
      assertSame(((ClientTransaction) list.getFirst()).getSequenceID(), writer.getMinTransactionSequence());
      i.remove();
      writer.removeTransaction(tx.getTransactionID());
    }
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
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    TestCommitTransactionMessageFactory mf = new TestCommitTransactionMessageFactory();
    ClientID clientID = new ClientID(new ChannelID(69));
    TxnBatchID batchID = new TxnBatchID(42);

    List tx1Notifies = new LinkedList();
    // A nested transaction (all this buys us is more than 1 lock in a txn)
    LockID lid1 = new LockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction tmp = new ClientTransactionImpl(new NullRuntimeLogger());
    tmp.setTransactionContext(tc);
    LockID lid2 = new LockID("2");
    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL, Arrays.asList(new LockID[] { lid1, lid2 }));
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);

    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    MockTCObject mtco = new MockTCObject(new ObjectID(2), this);
    mtco.setNew(true);

    txn1.createObject(mtco);
    txn1.createRoot("root", new ObjectID(3));
    for (int i = 0; i < 10; i++) {
      Notify notify = new Notify(new LockID("" + i), new ThreadID(i), i % 2 == 0);
      tx1Notifies.add(notify);
      txn1.addNotify(notify);
    }

    tc = new TransactionContextImpl(new LockID("3"), TxnType.CONCURRENT, TxnType.CONCURRENT);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);

    writer = new TransactionBatchWriter(GroupID.NULL_ID, batchID, serializer, encoding, mf, FoldingConfig
        .createFromProperties(TCPropertiesImpl.getProperties()));

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();

    writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    writer.addTransaction(txn2, sequenceGenerator, tidGenerator);

    DSOGlobalServerStats stats = getDSOGlobalServerStats();
    TransactionBatchReaderImpl reader = new TransactionBatchReaderImpl(writer.getData(), clientID, serializer,
                                                                       new ActiveServerTransactionFactory(), stats);
    // let transactionSize counter sample
    ThreadUtil.reallySleep(2000);
    assertTransactionSize(writer.getData(), 2, stats.getTransactionSizeCounter());
    
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
          assertTrue(Arrays.equals(new LockID[] { new LockID("1"), new LockID("2") }, txn.getLockIDs()));
          assertEquals(tx1Notifies, txn.getNotifies());
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

  private DSOGlobalServerStats getDSOGlobalServerStats() {
    CounterManager counterManager = new CounterManagerImpl();
    SampledRateCounter transactionSizeCounter = (SampledRateCounter) counterManager
        .createCounter(new SampledRateCounterConfig(1, 10, true));
    DSOGlobalServerStats stats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null, null,
                                                              null, transactionSizeCounter, null);
    return stats;
  }

  public void testSimpleFold() throws IOException {
    ObjectStringSerializer serializer = new ObjectStringSerializer();

    writer = newWriter(serializer, true, 0, 0);

    ClientID clientID = new ClientID(new ChannelID(69));

    LockID lid1 = new LockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    // txn3 has more objects than 1 & 2, but contains all from the previous, it can be folded
    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn3.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertTrue(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertTrue(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    // this txn does not share a common lock with the others (even though it has a common object) -- it
    // should not be folded
    LockID lid2 = new LockID("2");
    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn4 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn4.setTransactionContext(tc);
    txn4.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    folded = writer.addTransaction(txn4, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());

    DSOGlobalServerStats stats = getDSOGlobalServerStats();
    TransactionBatchReaderImpl reader = new TransactionBatchReaderImpl(writer.getData(), clientID, serializer,
                                                                       new ActiveServerTransactionFactory(), stats);
    // let transactionSize counter sample
    ThreadUtil.reallySleep(2000);
    assertTransactionSize(writer.getData(), 2, stats.getTransactionSizeCounter());
    
    assertEquals(2, reader.getNumberForTxns());
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
          assertEquals(3, txn.getNumApplicationTxn());
          assertEquals(0, txn.getNewRoots().size());
          assertEquals(2, txn.getObjectIDs().size());
          assertTrue(txn.getObjectIDs().containsAll(Arrays.asList(new ObjectID[] { new ObjectID(1), new ObjectID(2) })));
          assertEquals(TxnType.NORMAL, txn.getTransactionType());
          assertTrue(Arrays.equals(new LockID[] { new LockID("1") }, txn.getLockIDs()));
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
          assertTrue(Arrays.equals(new LockID[] { new LockID("2") }, txn.getLockIDs()));
          assertEquals(Collections.EMPTY_LIST, txn.getNotifies());
          break;
        default:
          fail("count is " + count);
      }
    }
  }

  private void assertTransactionSize(TCByteBuffer[] actualData, int actualNumTxns,
                                     SampledRateCounter transactionSizeCounter) {
    int expectedAvgTxnSize = new TCByteBufferInputStream(actualData).getTotalLength() / actualNumTxns;
    int actualAvgTxnSize = 0;
    int nonZeroSamples = 0;
    for (TimeStampedCounterValue val : transactionSizeCounter.getAllSampleValues()) {
      // regardless of how many samples, there should be only one sample with a non-zero value
      actualAvgTxnSize += val.getCounterValue();
      if (val.getCounterValue() != 0) nonZeroSamples++;
    }
    assertEquals(1, nonZeroSamples);
    assertEquals(expectedAvgTxnSize, actualAvgTxnSize);
  }

  public void testFoldObjectLimit() {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 0, 2);

    LockID lid1 = new LockID("1");
    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn1.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn1.fieldChanged(new MockTCObject(new ObjectID(3), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn2.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn2.fieldChanged(new MockTCObject(new ObjectID(3), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    // txn1 and txn2 exceed the object limit (should not fold)
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldLockLimit() {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 2, 0);

    LockID lid1 = new LockID("1");
    LockID lid2 = new LockID("2");
    LockID lid3 = new LockID("3");
    List threeLocks = Arrays.asList(lid1, lid2, lid3);

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL, threeLocks);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL, threeLocks);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    // txn1 and txn2 exceed the lock limit (should not fold)
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldDisabled() {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, false, 0, 0);

    LockID lid1 = new LockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    // folding disabled (these txns would normally fold)
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testDisallowedFolds() {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 0, 0);

    LockID lid1 = new LockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txnWithRoot = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithRoot.setTransactionContext(tc);
    txnWithRoot.createRoot("root", new ObjectID(234));

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txnWithDMI = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithDMI.setTransactionContext(tc);
    txnWithDMI.addDmiDescritor(new DmiDescriptor(new ObjectID(12), new ObjectID(13), new DmiClassSpec[] {}, true));

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txnWithNotify = new ClientTransactionImpl(new NullRuntimeLogger());
    txnWithNotify.setTransactionContext(tc);
    txnWithNotify.addNotify(new Notify(lid1, new ThreadID(122), true));

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    // Txns with DMI, root or notifies do not qualify for folds
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txnWithRoot, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txnWithDMI, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(3 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txnWithNotify, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(4 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testFoldBug1() {
    // Consider these 3 txns...
    // (txn1) - Lock1, Obj1(delta)
    // (txn2) - Lock2, Obj2(new)
    // (txn3) - Lock1, Obj1(delta), Obj2(delta)
    //
    // txn3 cannot be folded into txn1 because it would put the Obj2 delta before the txn that creates it (txn2)

    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 0, 0);

    LockID lid1 = new LockID("1");
    LockID lid2 = new LockID("2");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    MockTCObject mtco = new MockTCObject(new ObjectID(2), new Object());
    mtco.setNew(true);
    txn2.createObject(mtco);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    txn3.fieldChanged(new MockTCObject(new ObjectID(2), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertFalse(folded);
  }

  public void testTxnWithNewObjCanBeFolded() {
    // (txn1) - Lock1, Obj1(delta)
    // (txn2) - Lock1, Obj1(delta), Obj2(new)
    //
    // txn2 should be folded into txn1 even though it contains a "new" object

    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 0, 0);

    LockID lid1 = new LockID("1");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);
    MockTCObject mtco = new MockTCObject(new ObjectID(2), new Object());
    mtco.setNew(true);
    txn2.createObject(mtco);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertTrue(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  public void testOrdering() {
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    writer = newWriter(serializer, true, 0, 0);

    LockID lid1 = new LockID("1");
    LockID lid2 = new LockID("2");

    TransactionContext tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn1 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn1.setTransactionContext(tc);
    txn1.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid2, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn2 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn2.setTransactionContext(tc);
    txn2.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    tc = new TransactionContextImpl(lid1, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn3 = new ClientTransactionImpl(new NullRuntimeLogger());
    txn3.setTransactionContext(tc);
    txn3.fieldChanged(new MockTCObject(new ObjectID(1), this), "class", "class.field", ObjectID.NULL_ID, -1);

    SequenceGenerator sequenceGenerator = new SequenceGenerator();
    TransactionIDGenerator tidGenerator = new TransactionIDGenerator();
    final long startSeq = sequenceGenerator.getCurrentSequence();

    boolean folded;

    // There is a common object between txn1 and txn2 (but differing locks). This should close txn1
    // and disallow folds into it
    folded = writer.addTransaction(txn1, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(1 + startSeq, sequenceGenerator.getCurrentSequence());

    folded = writer.addTransaction(txn2, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(2 + startSeq, sequenceGenerator.getCurrentSequence());

    folded = writer.addTransaction(txn3, sequenceGenerator, tidGenerator);
    assertFalse(folded);
    assertEquals(3 + startSeq, sequenceGenerator.getCurrentSequence());
  }

  static class BatchWriterProperties implements TCProperties {
    private final int     objectLimit;
    private final int     lockLimit;
    private final boolean foldEnabled;

    BatchWriterProperties(boolean foldEnabled, int lockLimit, int objectLimit) {
      this.foldEnabled = foldEnabled;
      this.lockLimit = lockLimit;
      this.objectLimit = objectLimit;
    }

    public Properties addAllPropertiesTo(Properties properties) {
      throw new AssertionError();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_ENABLED.equals(key)) { return foldEnabled; }

      throw new AssertionError("key: " + key);
    }

    public boolean getBoolean(String key) {
      throw new AssertionError();
    }

    public float getFloat(String key) {
      throw new AssertionError();
    }

    public int getInt(String key, int defaultValue) {
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT.equals(key)) { return lockLimit; }
      if (TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT.equals(key)) { return objectLimit; }
      throw new AssertionError("key: " + key);
    }

    public int getInt(String key) {
      throw new AssertionError();
    }

    public long getLong(String key) {
      throw new AssertionError();
    }

    public TCProperties getPropertiesFor(String key) {
      throw new AssertionError();
    }

    public String getProperty(String key, boolean missingOkay) {
      throw new AssertionError();
    }

    public String getProperty(String key) {
      throw new AssertionError();
    }

    public long getLong(String key, long defaultValue) {
      throw new AssertionError();
    }

    public void overwriteTcPropertiesFromConfig(TcProperty[] tcProperties) {
      throw new AssertionError();
    }

    public void setProperty(String key, String value) {
      throw new AssertionError();
    }

  }

}
