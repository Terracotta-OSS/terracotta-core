/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.mockito.Mockito;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.net.MockChannel;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.stats.counter.Counter;
import com.tc.stats.counter.CounterConfig;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class RemoteTransactionManagerTest extends TestCase {

  private static final TCLogger        logger = TCLogging.getLogger(RemoteTransactionManagerTest.class);

  private RemoteTransactionManagerImpl manager;
  private TestTransactionBatchFactory  batchFactory;
  private SynchronizedInt              number;
  private SynchronizedRef              error;
  private LinkedQueue                  batchSendQueue;
  private TransactionBatchAccounting   batchAccounting;
  private CounterManager               counterManager;
  private SampledRateCounter           transactionsPerBatchCounter, transactionSizeCounter;
  private Counter                      outstandingBatchCounter, pendingBatchesSize;

  @Override
  public void setUp() throws Exception {
    this.batchFactory = new TestTransactionBatchFactory();
    this.counterManager = new CounterManagerImpl();
    this.transactionSizeCounter = (SampledRateCounter) this.counterManager
        .createCounter(new SampledRateCounterConfig(1, 900, true));
    this.transactionsPerBatchCounter = (SampledRateCounter) this.counterManager
        .createCounter(new SampledRateCounterConfig(1, 900, true));
    this.outstandingBatchCounter = this.counterManager.createCounter(new CounterConfig(0));
    this.pendingBatchesSize = this.counterManager.createCounter(new CounterConfig(0));

    this.manager = new RemoteTransactionManagerImpl(GroupID.NULL_ID, logger, this.batchFactory,
                                                    new TransactionIDGenerator(), new NullSessionManager(),
                                                    new MockChannel(), this.outstandingBatchCounter,
                                                    this.pendingBatchesSize, this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, 0);
    this.batchAccounting = this.manager.getBatchAccounting();
    this.number = new SynchronizedInt(0);
    this.error = new SynchronizedRef(null);
    this.batchSendQueue = new LinkedQueue();
  }

  @Override
  public void tearDown() throws Exception {
    if (this.error.get() != null) {
      Throwable t = (Throwable) this.error.get();
      fail(t.getMessage());
    }
    for (Iterator i = this.batchAccounting.addIncompleteTransactionIDsTo(new LinkedList()).iterator(); i.hasNext();) {
      TransactionID txID = (TransactionID) i.next();
      this.manager.receivedAcknowledgement(SessionID.NULL_ID, txID, GroupID.NULL_ID);
    }
    this.batchAccounting.clear();
    this.batchAccounting.stop();
    this.manager.clear();
  }

  public void testAckOnExitTimeoutFinite() throws Exception {

    final int ackOnExitTimeout = 10;
    this.manager = new RemoteTransactionManagerImpl(GroupID.NULL_ID, logger, this.batchFactory,
                                                    new TransactionIDGenerator(), new NullSessionManager(),
                                                    new MockChannel(), this.outstandingBatchCounter,
                                                    this.pendingBatchesSize, this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, ackOnExitTimeout * 1000);
    this.batchAccounting = this.manager.getBatchAccounting();

    final LockID lockID1 = new StringLockID("lock1");
    this.manager.flush(lockID1);
    TestClientTransaction tx1 = new TestClientTransaction();
    tx1.lockID = lockID1;
    tx1.allLockIDs.add(lockID1);
    tx1.txnType = TxnType.NORMAL;

    this.manager.commit(tx1);

    final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
    Runnable stoper = new Runnable() {
      public void run() {
        RemoteTransactionManagerTest.this.manager.stop(true);
        flushCalls.put(lockID1);
        System.err.println("Manager stopped");
      }
    };

    new Thread(stoper).start();
    int timeout = ackOnExitTimeout * 1000;
    System.err.println("Waiting for " + timeout);
    Object o = flushCalls.poll(timeout + 5000);
    assertNotNull(o);
  }

  public void testAckOnExitTimeoutINFinite() throws Exception {

    final int ackOnExitTimeout = 0;
    this.manager = new RemoteTransactionManagerImpl(GroupID.NULL_ID, logger, this.batchFactory,
                                                    new TransactionIDGenerator(), new NullSessionManager(),
                                                    new MockChannel(), this.outstandingBatchCounter,
                                                    this.pendingBatchesSize, this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, ackOnExitTimeout * 1000);
    this.batchAccounting = this.manager.getBatchAccounting();

    final LockID lockID1 = new StringLockID("lock1");
    this.manager.flush(lockID1);
    TestClientTransaction tx1 = new TestClientTransaction();
    tx1.lockID = lockID1;
    tx1.allLockIDs.add(lockID1);
    tx1.txnType = TxnType.NORMAL;

    this.manager.commit(tx1);

    final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
    Runnable stoper = new Runnable() {
      public void run() {
        RemoteTransactionManagerTest.this.manager.stop(true);
        flushCalls.put(lockID1);
        System.err.println("Manager stopped");
      }
    };

    new Thread(stoper).start();
    int timeout = 30 * 1000;
    System.err.println("Waiting for " + timeout);
    Object o = flushCalls.poll(timeout + 5000);
    assertNull(o);
  }

  public void testFlush() throws Exception {
    final LockID lockID1 = new StringLockID("lock1");
    this.manager.flush(lockID1);
    TestClientTransaction tx1 = new TestClientTransaction();
    tx1.lockID = lockID1;
    tx1.allLockIDs.add(lockID1);
    tx1.txnType = TxnType.NORMAL;
    this.manager.commit(tx1);
    final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
    Runnable flusher = new Runnable() {
      public void run() {
        RemoteTransactionManagerTest.this.manager.flush(lockID1);
        flushCalls.put(lockID1);
      }
    };

    new Thread(flusher).start();
    // XXX: Figure out how to do this without a timeout.
    int timeout = 5 * 1000;
    System.err.println("About to wait for " + timeout + " ms.");

    Object o = flushCalls.poll(timeout);

    assertNull(o);

    this.manager.receivedAcknowledgement(SessionID.NULL_ID, tx1.getTransactionID(), GroupID.NULL_ID);
    assertEquals(lockID1, flushCalls.take());

    TestClientTransaction tx2 = new TestClientTransaction();
    tx2.lockID = lockID1;
    tx2.allLockIDs.add(lockID1);
    tx2.txnType = TxnType.NORMAL;

    // make sure flush falls through if the acknowledgment is received before the flush is called.
    this.manager.commit(tx2);
    this.manager.receivedAcknowledgement(SessionID.NULL_ID, tx2.getTransactionID(), GroupID.NULL_ID);
    new Thread(flusher).start();
    assertEquals(lockID1, flushCalls.take());
  }

  public void testSendAckedGlobalTransactionIDs() throws Exception {
    assertTrue(this.batchSendQueue.isEmpty());
    ClientTransaction ctx = makeTransaction();
    CyclicBarrier barrier = new CyclicBarrier(2);

    callCommitOnThread(ctx, barrier);
    barrier.barrier();
    ThreadUtil.reallySleep(500);
    TestTransactionBatch batch = (TestTransactionBatch) this.batchFactory.newBatchQueue.poll(1);
    assertNotNull(batch);
    assertSame(batch, this.batchSendQueue.poll(1));
    assertTrue(this.batchSendQueue.isEmpty());

    // fill the current batch with a bunch of transactions
    int count = 50;
    for (int i = 0; i < count; i++) {
      ClientTransaction ctx1 = makeTransaction();
      barrier = new CyclicBarrier(2);
      callCommitOnThread(ctx1, barrier);
      barrier.barrier();
      ThreadUtil.reallySleep(500);
    }

    List batches = new ArrayList();
    TestTransactionBatch batch1;
    while ((batch1 = (TestTransactionBatch) this.batchSendQueue.poll(3000)) != null) {
      System.err.println("Recd batch " + batch1);
      batches.add(batch1);
    }

    // acknowledge the first transaction
    TransactionID tid = ctx.getTransactionID();
    assertFalse(tid.isNull());
    this.manager.receivedAcknowledgement(SessionID.NULL_ID, tid, GroupID.NULL_ID);

    this.manager.receivedBatchAcknowledgement(batch.batchID, GroupID.NULL_ID);

    // the batch ack should have sent another batch
    batch = (TestTransactionBatch) this.batchSendQueue.poll(1);
    assertNotNull(batch);
    assertTrue(this.batchSendQueue.isEmpty());

    ctx = makeTransaction();
    barrier = new CyclicBarrier(2);
    callCommitOnThread(ctx, barrier);
    barrier.barrier();
    ThreadUtil.reallySleep(500);

    // acknowledge the remaining batches so the current batch will get sent.
    for (Iterator i = batches.iterator(); i.hasNext();) {
      batch1 = (TestTransactionBatch) i.next();
      this.manager.receivedBatchAcknowledgement(batch1.batchID, GroupID.NULL_ID);
    }
    this.manager.receivedBatchAcknowledgement(batch.batchID, GroupID.NULL_ID);

    batch = (TestTransactionBatch) this.batchSendQueue.poll(1);
    assertNotNull(batch);
    assertTrue(this.batchSendQueue.isEmpty());
  }

  public void testResendOutstandingBasics() throws Exception {
    System.err.println("Testing testResendOutstandingBasics ...");
    final Set batchTxs = new HashSet();

    final int maxBatchesOutstanding = this.manager.getMaxOutStandingBatches();
    final List batches = new ArrayList();

    TestTransactionBatch batchN;
    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = (TestTransactionBatch) this.batchSendQueue.take();
      System.err.println("* Recd " + batchN);
      assertEquals(batchN, getNextNewBatch());
      assertTrue(this.batchSendQueue.isEmpty());
      batches.add(batchN);
      assertEquals(1, batchN.transactions.size());
    }

    final int num = 5;
    // These txns are not gonna be sent as we already reached the max Batches outstanding count
    makeAndCommitTransactions(batchTxs, num);
    ThreadUtil.reallySleep(2000);
    assertTrue(this.batchSendQueue.isEmpty());

    // Resend outstanding batches
    restart(this.manager);

    // Make sure the batches get resent
    for (int i = batches.size(); i > 0; i--) {
      assertTrue(batches.contains(this.batchSendQueue.take()));
    }
    assertTrue(this.batchSendQueue.isEmpty());

    // ACK batch 1; next batch (batch 3) will be sent.
    this.manager.receivedBatchAcknowledgement(((TestTransactionBatch) batches.get(0)).batchID, GroupID.NULL_ID);
    while ((batchN = (TestTransactionBatch) this.batchSendQueue.poll(3000)) != null) {
      System.err.println("** Recd " + batchN);
      batches.add(batchN);
      getNextNewBatch();
    }

    // Resend outstanding batches
    restart(this.manager);

    // This time, all batches + batch 3 should get resent
    List sent = (List) drainQueueInto(this.batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // make some new transactions that should go into next batch
    makeAndCommitTransactions(batchTxs, num);

    // ACK all the transactions in batch1
    Collection batch1Txs = ((TestTransactionBatch) batches.get(0)).addTransactionIDsTo(new HashSet());
    for (Iterator i = batch1Txs.iterator(); i.hasNext();) {
      TransactionID txnId = (TransactionID) i.next();
      batchTxs.remove(txnId);
      this.manager.receivedAcknowledgement(SessionID.NULL_ID, txnId, GroupID.NULL_ID);
    }
    batches.remove(0);

    // re-send
    restart(this.manager);

    // This time, batches except batch 1 should get resent
    sent = (List) drainQueueInto(this.batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // ACK all other batches
    for (Iterator i = batches.iterator(); i.hasNext();) {
      batchN = (TestTransactionBatch) i.next();
      this.manager.receivedBatchAcknowledgement(batchN.batchID, GroupID.NULL_ID);
    }

    while ((batchN = (TestTransactionBatch) this.batchSendQueue.poll(3000)) != null) {
      System.err.println("*** Recd " + batchN);
      batches.add(batchN);
      getNextNewBatch();
    }

    // resend
    restart(this.manager);

    // This time, batches except batch 1 should get resent
    sent = (List) drainQueueInto(this.batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // now make sure that the manager re-sends an outstanding batch until all of
    // its transactions have been ACKed.
    while (batches.size() > 0) {
      Collection batchNTxs = ((TestTransactionBatch) batches.get(0)).addTransactionIDsTo(new HashSet());
      for (Iterator i = batchNTxs.iterator(); i.hasNext();) {
        TransactionID txnId = (TransactionID) i.next();
        batchTxs.remove(txnId);
        this.manager.receivedAcknowledgement(SessionID.NULL_ID, txnId, GroupID.NULL_ID);
        restart(this.manager);
        sent = (List) drainQueueInto(this.batchSendQueue, new LinkedList());
        if (i.hasNext()) {
          // There are still un-ACKed transactions in this batch.
          assertEquals(batches.size(), sent.size());
          assertTrue(batches.containsAll(sent));
        } else {
          // all the transactions have been ACKed, so current batch (batch 4 should be sent)
          batches.remove(0);
          assertEquals(batches.size(), sent.size());
          assertTrue(batches.containsAll(sent));
        }
      }
    }
  }

  private void restart(RemoteTransactionManagerImpl manager2) {
    manager2.pause(GroupID.ALL_GROUPS, 1);
    manager2.unpause(GroupID.ALL_GROUPS, 0);

  }

  private void makeAndCommitTransactions(final Set created, final int count) throws InterruptedException {
    CyclicBarrier commitBarrier = new CyclicBarrier(count + 1);
    for (int i = 0; i < count; i++) {
      ClientTransaction tx = makeTransaction();
      created.add(tx);
      callCommitOnThread(tx, commitBarrier);
    }
    // make sure all the threads have at least started...
    commitBarrier.barrier();
    // sleep a little bit to make sure they get to the commit() call.
    ThreadUtil.reallySleep(1000);
  }

  public void testBatching() throws InterruptedException {

    System.err.println("Testing testBatching ...");

    final int maxBatchesOutstanding = this.manager.getMaxOutStandingBatches();
    TestTransactionBatch batchN;
    final Set batchTxs = new HashSet();
    final List batches = new ArrayList();

    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = (TestTransactionBatch) this.batchSendQueue.take();
      System.err.println("* Recd " + batchN);
      assertEquals(batchN, getNextNewBatch());
      assertTrue(this.batchSendQueue.isEmpty());
      batches.add(batchN);
      assertEquals(1, batchN.transactions.size());
      assertTrue(batchTxs.containsAll(batchN.transactions));
    }

    final int num = 10;

    // create more transactions on the client side (they should all get batched
    // locally)
    List batch2Txs = new ArrayList();
    CyclicBarrier barrier = new CyclicBarrier(num + 1);
    for (int i = 1; i <= num; i++) {
      ClientTransaction txn = makeTransaction();
      batch2Txs.add(txn);
      callCommitOnThread(txn, barrier);
    }
    batchTxs.addAll(batch2Txs);

    barrier.barrier();
    assertFalse(barrier.broken());
    ThreadUtil.reallySleep(2000);
    assertTrue(this.batchSendQueue.isEmpty());

    // Make sure the rest transactions get into the second batch
    TestTransactionBatch batch2 = getNextNewBatch();
    Collection txnsInBatch = drainQueueInto(batch2.addTxQueue, new HashSet());
    assertTrue(batch2Txs.size() == txnsInBatch.size());
    txnsInBatch.removeAll(batch2Txs);
    assertTrue(txnsInBatch.size() == 0);
    assertTrue(batch2.addTxQueue.isEmpty());

    TestTransactionBatch batch1 = ((TestTransactionBatch) batches.remove(0));

    // ACK one of the batch (triggers send of next batch)
    this.manager.receivedBatchAcknowledgement(batch1.batchID, GroupID.NULL_ID);
    // make sure that the batch sent is what we expected.
    assertSame(batch2, this.batchSendQueue.take());

    TestTransactionBatch batch3 = getNextNewBatch();

    // ACK another batch (no more TXNs to send this time)
    assertTrue(this.batchSendQueue.isEmpty());
    this.manager.receivedBatchAcknowledgement(batch2.batchID, GroupID.NULL_ID);
    assertTrue(this.batchSendQueue.isEmpty());
    for (Iterator i = batches.iterator(); i.hasNext();) {
      TestTransactionBatch b = (TestTransactionBatch) i.next();
      this.manager.receivedBatchAcknowledgement(b.batchID, GroupID.NULL_ID);
      assertTrue(this.batchSendQueue.isEmpty());
    }

    for (Iterator i = batchTxs.iterator(); i.hasNext();) {
      ClientTransaction txn = (ClientTransaction) i.next();
      TransactionID tid = txn.getTransactionID();
      assertFalse(tid.isNull());
      this.manager.receivedAcknowledgement(SessionID.NULL_ID, tid, GroupID.NULL_ID);
      assertTrue(this.batchSendQueue.isEmpty());
    }

    // There should still be no batch to send.
    assertTrue(this.batchSendQueue.isEmpty());
    assertTrue(drainQueueInto(batch3.addTxQueue, new LinkedList()).isEmpty());
  }

  private Collection drainQueueInto(LinkedQueue queue, Collection dest) throws InterruptedException {
    while (!queue.isEmpty()) {
      dest.add(queue.take());
    }
    return dest;
  }

  private TestTransactionBatch getNextNewBatch() throws InterruptedException {
    TestTransactionBatch rv = (TestTransactionBatch) this.batchFactory.newBatchQueue.take();
    return rv;
  }

  private synchronized void callCommitOnThread(final ClientTransaction txn, final CyclicBarrier barrier) {

    Thread t = new Thread("Commit for txn #" + txn) {
      @Override
      public void run() {
        try {
          barrier.barrier();
          RemoteTransactionManagerTest.this.manager.commit(txn);
        } catch (Throwable th) {
          th.printStackTrace();
          RemoteTransactionManagerTest.this.error.set(th);
        }
      }
    };

    t.start();
  }

  private ClientTransaction makeTransaction() {
    int num = this.number.increment();
    LockID lid = new StringLockID("lock" + num);
    TransactionContext tc = new TransactionContextImpl(lid, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn = new ClientTransactionImpl(new NullRuntimeLogger());
    txn.setTransactionContext(tc);
    txn.fieldChanged(new MockTCObject(new ObjectID(num), this), "class", "class.field", new ObjectID(num), -1);
    return txn;
  }

  private final class TestTransactionBatch implements ClientTransactionBatch {

    public final TxnBatchID  batchID;

    public final LinkedQueue addTxQueue   = new LinkedQueue();
    private final LinkedList transactions = new LinkedList();

    public TestTransactionBatch(TxnBatchID batchID) {
      this.batchID = batchID;
    }

    @Override
    public String toString() {
      return "TestTransactionBatch[" + this.batchID + "] = Txn [ " + this.transactions + " ]";
    }

    public synchronized boolean isEmpty() {
      return this.transactions.isEmpty();
    }

    public synchronized int numberOfTxnsBeforeFolding() {
      return this.transactions.size();
    }

    public boolean isNull() {
      return false;
    }

    public synchronized FoldedInfo addTransaction(ClientTransaction txn, SequenceGenerator sequenceGenerator,
                                                  TransactionIDGenerator transactionIDGenerator) {
      txn.setSequenceID(new SequenceID(sequenceGenerator.getNextSequence()));
      txn.setTransactionID(transactionIDGenerator.nextTransactionID());
      try {
        this.addTxQueue.put(txn);
        this.transactions.add(txn);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      return new FoldedInfo(null, false);
    }

    public TransactionBuffer removeTransaction(TransactionID txID) {
      return Mockito.mock(TransactionBuffer.class);
    }

    public synchronized Collection addTransactionIDsTo(Collection c) {
      for (Iterator i = this.transactions.iterator(); i.hasNext();) {
        ClientTransaction txn = (ClientTransaction) i.next();
        c.add(txn.getTransactionID());
      }
      return c;
    }

    public void send() {
      try {
        RemoteTransactionManagerTest.this.batchSendQueue.put(this);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    public TCByteBuffer[] getData() {
      return null;
    }

    public TxnBatchID getTransactionBatchID() {
      return this.batchID;
    }

    public SequenceID getMinTransactionSequence() {
      throw new ImplementMe();
    }

    public void recycle() {
      return;
    }

    public synchronized Collection addTransactionSequenceIDsTo(Collection sequenceIDs) {
      for (Iterator i = this.transactions.iterator(); i.hasNext();) {
        ClientTransaction txn = (ClientTransaction) i.next();
        sequenceIDs.add(txn.getSequenceID());
      }
      return sequenceIDs;
    }

    public String dump() {
      return "TestTransactionBatch";
    }

    public int byteSize() {
      return 64000;
    }

  }

  private final class TestTransactionBatchFactory implements TransactionBatchFactory {
    private long             idSequence;
    public final LinkedQueue newBatchQueue = new LinkedQueue();

    public ClientTransactionBatch nextBatch(GroupID groupID) {
      ClientTransactionBatch rv = new TestTransactionBatch(new TxnBatchID(++this.idSequence));
      try {
        this.newBatchQueue.put(rv);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      return rv;
    }

  }

}
