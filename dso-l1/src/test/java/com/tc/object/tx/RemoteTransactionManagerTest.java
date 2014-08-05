/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.mockito.Mockito;

import com.tc.abortable.AbortedOperationException;
import com.tc.abortable.NullAbortableOperationManager;
import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.net.MockChannel;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.Runners;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class RemoteTransactionManagerTest extends TestCase {

  private static final TCLogger                     logger      = TCLogging
                                                                    .getLogger(RemoteTransactionManagerTest.class);

  private RemoteTransactionManagerImpl              manager;
  private TestTransactionBatchFactory               batchFactory;
  private AtomicInteger                             number;
  private AtomicReference                           error;
  private LinkedBlockingQueue<TestTransactionBatch> batchSendQueue;
  private TransactionBatchAccounting                batchAccounting;
  private CounterManager                            counterManager;
  private SampledRateCounter                        transactionsPerBatchCounter, transactionSizeCounter;

  private final TCThreadGroup                       threadGroup = new TCThreadGroup(
                                                                                    new ThrowableHandlerImpl(
                                                                                                             TCLogging
                                                                                                                 .getLogger(RemoteTransactionManagerTest.class)));
  private final TaskRunner                          taskRunner  = Runners
                                                                    .newSingleThreadScheduledTaskRunner(threadGroup);

  @Override
  public void setUp() throws Exception {
    this.batchFactory = new TestTransactionBatchFactory(false);
    this.counterManager = new CounterManagerImpl();
    this.transactionSizeCounter = (SampledRateCounter) this.counterManager
        .createCounter(new SampledRateCounterConfig(1, 900, true));
    this.transactionsPerBatchCounter = (SampledRateCounter) this.counterManager
        .createCounter(new SampledRateCounterConfig(1, 900, true));

    this.manager = new RemoteTransactionManagerImpl(GroupID.NULL_ID, logger, this.batchFactory,
                                                    new TransactionIDGenerator(), new NullSessionManager(),
                                                    new MockChannel(), this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, 0,
                                                    new NullAbortableOperationManager(), taskRunner);
    this.manager.setFixedBatchSize(10);
    this.batchAccounting = this.manager.getBatchAccounting();
    this.number = new AtomicInteger(0);
    this.error = new AtomicReference(null);
    this.batchSendQueue = new LinkedBlockingQueue();
  }

  @Override
  public void tearDown() throws Exception {
    if (this.error.get() != null) {
      Throwable t = (Throwable) this.error.get();
      fail(t.getMessage());
    }
    for (Object o : this.batchAccounting.addIncompleteTransactionIDsTo(new LinkedList())) {
      TransactionID txID = (TransactionID) o;
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
                                                    new MockChannel(), this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, ackOnExitTimeout * 1000,
                                                    new NullAbortableOperationManager(), taskRunner);
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
      @Override
      public void run() {
        RemoteTransactionManagerTest.this.manager.stop();
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
                                                    new MockChannel(), this.transactionSizeCounter,
                                                    this.transactionsPerBatchCounter, ackOnExitTimeout * 1000,
                                                    new NullAbortableOperationManager(), taskRunner);
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
      @Override
      public void run() {
        RemoteTransactionManagerTest.this.manager.stop();
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
      @Override
      public void run() {
        try {
          RemoteTransactionManagerTest.this.manager.flush(lockID1);
        } catch (AbortedOperationException e) {
          e.printStackTrace();
          // should never come here
        }
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
    this.manager.waitForPendings();
    this.manager.receivedAcknowledgement(SessionID.NULL_ID, tx2.getTransactionID(), GroupID.NULL_ID);
    new Thread(flusher).start();
    assertEquals(lockID1, flushCalls.take());
  }

  public void testSendAckedGlobalTransactionIDs() throws Exception {
    assertTrue(this.batchSendQueue.isEmpty());
    ClientTransaction ctx = makeTransaction();
    CyclicBarrier barrier = new CyclicBarrier(2);

    callCommitOnThread(ctx, barrier);
    barrier.await();
    TimeUnit.SECONDS.sleep(1L);
    TestTransactionBatch batch = (TestTransactionBatch) this.batchFactory.newBatchQueue.poll();
    assertNotNull(batch);
    assertFalse(this.batchSendQueue.isEmpty());
    List<TestTransactionBatch> ackd = ackBatchesThatAreSent();
    assertEquals(1, ackd.size());
    assertSame(batch, ackd.get(0));
    assertTrue(this.batchSendQueue.isEmpty());

    // fill the current batch with a bunch of transactions
    int count = 50;
    for (int i = 0; i < count; i++) {
      ClientTransaction ctx1 = makeTransaction();
      barrier = new CyclicBarrier(2);
      callCommitOnThread(ctx1, barrier);
      barrier.await();
      ThreadUtil.reallySleep(500);
    }

    // acknowledge the first transaction
    TransactionID tid = ctx.getTransactionID();
    assertFalse(tid.isNull());
    this.manager.receivedAcknowledgement(SessionID.NULL_ID, tid, GroupID.NULL_ID);

    int queueSize = this.batchSendQueue.size();
    int batchCount = ackBatchesThatAreSent().size();

    // the batch ack should have sent another batch
    assertTrue(batchCount >= queueSize);
    assertTrue(this.batchSendQueue.isEmpty());

    ctx = makeTransaction();
    barrier = new CyclicBarrier(2);
    callCommitOnThread(ctx, barrier);
    barrier.await();
    ThreadUtil.reallySleep(2000);
    // acknowledge the remaining batches so the current batch will get sent.
    batchCount = ackBatchesThatAreSent().size();

    assertEquals(1, batchCount);
    assertTrue(this.batchSendQueue.isEmpty());
  }

  public void testResendOutstandingBasics() throws Exception {
    System.err.println("Testing testResendOutstandingBasics ...");
    final Set batchTxs = new HashSet();

    final int maxBatchesOutstanding = this.manager.getMaxOutStandingBatches();
    final List<TestTransactionBatch> batches = new ArrayList<TestTransactionBatch>();

    TestTransactionBatch batchN;
    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = getNextSentBatch();
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

    Collection<TestTransactionBatch> ackd = restart(this.manager);

    // Make sure the batches get resent
    for (TestTransactionBatch batch : batches) {
      assertTrue(ackd.remove(batch));
    }

    // ACK batch 1; next batch (batch 3) will be sent.
    for (TestTransactionBatch batch : ackd) {
      System.err.println("** Recd " + batch);
      batches.add(batch);
    }

    // Resend outstanding batches
    ackd = restart(this.manager);

    // This time, all batches + batch 3 should get resent
    for (TestTransactionBatch batch : batches) {
      assertTrue(ackd.remove(batch));
    }
    assertTrue(ackd.isEmpty());

    // ACK all the transactions in batch1
    Collection batch1Txs = batches.get(0).addTransactionIDsTo(new HashSet());
    for (Iterator i = batch1Txs.iterator(); i.hasNext();) {
      TransactionID txnId = (TransactionID) i.next();
      batchTxs.remove(txnId);
      this.manager.receivedAcknowledgement(SessionID.NULL_ID, txnId, GroupID.NULL_ID);
    }
    batches.remove(0);

    // re-send
    List<TestTransactionBatch> sent = restart(this.manager);

    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // now make sure that the manager re-sends an outstanding batch until all of
    // its transactions have been ACKed.
    while (batches.size() > 0) {
      Collection batchNTxs = batches.get(0).addTransactionIDsTo(new HashSet());
      for (Iterator i = batchNTxs.iterator(); i.hasNext();) {
        TransactionID txnId = (TransactionID) i.next();
        batchTxs.remove(txnId);
        this.manager.receivedAcknowledgement(SessionID.NULL_ID, txnId, GroupID.NULL_ID);
        sent = restart(this.manager);
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

  private TestTransactionBatch getNextSentBatch() throws InterruptedException {
    manager.waitForPendings();
    return this.batchSendQueue.take();
  }

  private List<TestTransactionBatch> restart(RemoteTransactionManagerImpl manager2) {
    manager2.pause(GroupID.ALL_GROUPS, 1);
    this.batchSendQueue.clear();
    final AtomicReference<List<TestTransactionBatch>> ret = new AtomicReference<List<TestTransactionBatch>>();
    // Resend outstanding batches
    Thread result = new Thread() {

      @Override
      public void run() {
        try {
          ret.set(ackBatchesThatAreSent());
        } catch (InterruptedException ie) {
          error.set(ie);
        }
      }

    };
    result.start();
    manager2.unpause(GroupID.ALL_GROUPS, 0);
    try {
      result.join();
    } catch (InterruptedException ie) {
      error.set(ie);
    }
    return ret.get();
  }

  private void makeAndCommitTransactions(final Set created, final int count) throws BrokenBarrierException,
      InterruptedException {
    CyclicBarrier commitBarrier = new CyclicBarrier(count + 1);
    for (int i = 0; i < count; i++) {
      ClientTransaction tx = makeTransaction();
      created.add(tx);
      callCommitOnThread(tx, commitBarrier);
    }
    // make sure all the threads have at least started...
    commitBarrier.await();
    // sleep a little bit to make sure they get to the commit() call.
    ThreadUtil.reallySleep(1000);
  }

  public void testBatching() throws BrokenBarrierException, InterruptedException {

    System.err.println("Testing testBatching ...");

    final int maxBatchesOutstanding = this.manager.getMaxOutStandingBatches();
    TestTransactionBatch batchN;
    final Set batchTxs = new HashSet();
    final List batches = new ArrayList();

    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = getNextSentBatch();
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

    barrier.await();
    assertFalse(barrier.isBroken());

    ThreadUtil.reallySleep(2000);
    assertTrue(this.batchSendQueue.isEmpty());

    // Make sure the rest transactions get into the second batch
    Set<ClientTransaction> heldTx = new HashSet<ClientTransaction>();
    Set<TestTransactionBatch> heldBatches = new HashSet<TestTransactionBatch>();
    while (heldTx.size() != num) {
      TestTransactionBatch batch2 = getNextNewBatch();
      drainQueueInto(batch2.addTxQueue, heldTx);
      heldBatches.add(batch2);
      assertTrue(heldBatches.size() <= num);
    }

    // make sure that the batch sent is what we expected.
    while (!heldBatches.isEmpty()) {
      TestTransactionBatch batch1 = ((TestTransactionBatch) batches.remove(0));
      // ACK one of the batch (triggers send of next batch)
      this.manager.receivedBatchAcknowledgement(batch1.batchID, GroupID.NULL_ID);
      assertTrue(heldBatches.remove(getNextSentBatch()));
    }

    TestTransactionBatch batch3 = getNextNewBatch();

    // ACK another batch (no more TXNs to send this time)
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

  private Collection drainQueueInto(LinkedBlockingQueue queue, Collection dest) {
    Object o = queue.poll();
    while (o != null) {
      dest.add(o);
      o = queue.poll();
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
          barrier.await();
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
    int num = this.number.incrementAndGet();
    LockID lid = new StringLockID("lock" + num);
    TransactionContext tc = new TransactionContextImpl(lid, TxnType.NORMAL, TxnType.NORMAL);
    ClientTransaction txn = new ClientTransactionImpl(0);
    txn.setTransactionContext(tc);
    txn.logicalInvoke(new MockTCObject(new ObjectID(num), this), LogicalOperation.CLEAR, new Object[] {},
                      new LogicalChangeID(num));

    return txn;
  }

  private List<TestTransactionBatch> ackBatchesThatAreSent() throws InterruptedException {
    TestTransactionBatch batch;
    List<TestTransactionBatch> ackd = new LinkedList<TestTransactionBatch>();
    while ((batch = this.batchSendQueue.poll(4, TimeUnit.SECONDS)) != null) {
      // TestTransactionBatch compare = (TestTransactionBatch)this.batchFactory.newBatchQueue.poll();
      // assertEquals(compare, batch);
      this.manager.receivedBatchAcknowledgement(batch.getTransactionBatchID(), GroupID.NULL_ID);
      ackd.add(batch);
    }
    return ackd;
  }

  private final class TestTransactionBatch implements ClientTransactionBatch {

    public final TxnBatchID          batchID;

    public final LinkedBlockingQueue addTxQueue   = new LinkedBlockingQueue();
    private final LinkedList         transactions = new LinkedList();

    public TestTransactionBatch(TxnBatchID batchID) {
      this.batchID = batchID;
    }

    @Override
    public String toString() {
      return "TestTransactionBatch[" + this.batchID + "] = Txn [ " + this.transactions + " ]";
    }

    @Override
    public synchronized boolean isEmpty() {
      return this.transactions.isEmpty();
    }

    @Override
    public synchronized int numberOfTxnsBeforeFolding() {
      return this.transactions.size();
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
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
      TransactionBuffer buffer = Mockito.mock(TransactionBuffer.class);
      Mockito.when(buffer.getTxnCount()).thenReturn(1);
      Mockito.when(buffer.getFoldedTransactionID()).thenReturn(null);
      return new FoldedInfo(buffer);
    }

    @Override
    public TransactionBuffer addSimpleTransaction(ClientTransaction txn) {
      try {
        this.addTxQueue.put(txn);
        this.transactions.add(txn);
        return Mockito.mock(TransactionBuffer.class);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    @Override
    public TransactionBuffer removeTransaction(TransactionID txID) {
      this.transactions.remove(txID);
      return Mockito.mock(TransactionBuffer.class);
    }

    @Override
    public boolean contains(TransactionID txID) {
      return transactions.contains(txID);
    }

    @Override
    public synchronized Collection addTransactionIDsTo(Collection c) {
      for (Iterator i = this.transactions.iterator(); i.hasNext();) {
        ClientTransaction txn = (ClientTransaction) i.next();
        c.add(txn.getTransactionID());
      }
      return c;
    }

    @Override
    public void send() {
      try {
        RemoteTransactionManagerTest.this.batchSendQueue.put(this);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    @Override
    public TCByteBuffer[] getData() {
      return null;
    }

    @Override
    public TxnBatchID getTransactionBatchID() {
      return this.batchID;
    }

    @Override
    public SequenceID getMinTransactionSequence() {
      throw new ImplementMe();
    }

    @Override
    public void recycle() {
      return;
    }

    @Override
    public synchronized Collection addTransactionSequenceIDsTo(Collection sequenceIDs) {
      for (Iterator i = this.transactions.iterator(); i.hasNext();) {
        ClientTransaction txn = (ClientTransaction) i.next();
        sequenceIDs.add(txn.getSequenceID());
      }
      return sequenceIDs;
    }

    @Override
    public String dump() {
      return "TestTransactionBatch";
    }

    @Override
    public int byteSize() {
      return 64000;
    }

  }

  private final class TestTransactionBatchFactory implements TransactionBatchFactory {
    private long                     idSequence;
    private final boolean            folding;
    public final LinkedBlockingQueue newBatchQueue = new LinkedBlockingQueue();

    public TestTransactionBatchFactory(boolean folding) {
      this.folding = folding;
    }

    @Override
    public ClientTransactionBatch nextBatch(GroupID groupID) {
      ClientTransactionBatch rv = new TestTransactionBatch(new TxnBatchID(++this.idSequence));
      try {
        this.newBatchQueue.put(rv);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      return rv;
    }

    @Override
    public boolean isFoldingSupported() {
      return folding;
    }
  }

}
