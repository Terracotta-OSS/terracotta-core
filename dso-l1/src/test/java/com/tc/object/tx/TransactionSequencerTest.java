/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.NullAbortableOperationManager;
import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.net.GroupID;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldedInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.stats.counter.sampled.derived.SampledRateCounterImpl;
import com.tc.util.CallableWaiter;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.Thread.State;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public class TransactionSequencerTest extends TestCase {

  public TransactionSequencer txnSequencer;
  public boolean  folding = true;
  private static final long   TIME_TO_RUN         = 1 * 60 * 1000;
  private static final int    MAX_PENDING_BATCHES = 5;

  @Override
  protected void setUp() throws Exception {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES,
                                                 MAX_PENDING_BATCHES + "");
    RemoteTransactionManagerImpl mockedRTMI = Mockito.mock(RemoteTransactionManagerImpl.class);
    this.txnSequencer = new TransactionSequencer(GroupID.NULL_ID, new TransactionIDGenerator(),
                                                 new TestTransactionBatchFactory(),
                                                 new TestLockAccounting(new NullAbortableOperationManager(), mockedRTMI),
                                                 new SampledRateCounterImpl(new SampledRateCounterConfig(1, 1, false)),
                                                 new SampledRateCounterImpl(new SampledRateCounterConfig(1, 1, false)),
                                                 new NullAbortableOperationManager(),
 mockedRTMI);
  }
  
  // checkout DEV-5872 to know why this test was written
  public void testDeadLockWithFolding() throws InterruptedException {
      folding = true;
      deadlockCheck();
  }
  // checkout DEV-5872 to know why this test was written
  public void testDeadLockWithNoFolding() throws InterruptedException {
      folding = false;
      deadlockCheck();
  }
  // checkout DEV-5872 to know why this test was written
  public void deadlockCheck() throws InterruptedException {
    Thread producer1 = new Thread(new Producer(this.txnSequencer), "Producer1");
    producer1.start();
    Thread producer2 = new Thread(new Producer(this.txnSequencer), "Producer2");
    producer2.start();
    Thread producer3 = new Thread(new Producer(this.txnSequencer), "Producer3");
    producer3.start();
    Thread producer4 = new Thread(new Producer(this.txnSequencer), "Producer4");
    producer4.start();
    Thread producer5 = new Thread(new Producer(this.txnSequencer), "Producer5");
    producer5.start();
    ThreadUtil.reallySleep(5000);

    Thread consumer = new Thread(new Consumer(this.txnSequencer), "Consumer");
    consumer.start();

    producer1.join();
    consumer.join();
  }
  public void testInterruptAddTransactionWithFolding() throws Exception {
      folding = true;
      interruptAddTransactionCheck();
  }
  public void testInterruptAddTransaction() throws Exception {
      folding = false;
      interruptAddTransactionCheck();
  }

  public void interruptAddTransactionCheck() throws Exception {
    // DEV-5589: Allow interrupting adding to the transaction sequencer.
      //  stuff the pending batches to the max
    for (int i = 0; i <= MAX_PENDING_BATCHES; i++) {
      this.txnSequencer.throttleIfNecesary();
      this.txnSequencer.addTransaction(new TestClientTransaction());
    }

    final AtomicBoolean failed = new AtomicBoolean(false);
    final Thread waiter = new Thread("waiter") {
      @Override
      public void run() {
        try {
          txnSequencer.throttleIfNecesary();
          txnSequencer.addTransaction(new TestClientTransaction());
        } catch (Exception e) {
          System.out.println("Got an exception adding to txnSequencer: " + e.getMessage());
          failed.set(true);
        }
        if (!interrupted()) {
          System.out.println("Thread was not interrupted.");
          failed.set(true);
        }
      }
    };
    waiter.setDaemon(true);
    waiter.start();

    System.out.println("Waiting for the thread to get into a wait state.");
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return waiter.getState() != State.RUNNABLE;
      }
    });

    System.out.println("Interrupting the thread.");
    waiter.interrupt();

    System.out.println("Unblocking the thread.");
    this.txnSequencer.getNextBatch();
    waiter.join();
    assertFalse(failed.get());
  }

  private static class Producer implements Runnable {
    private final TransactionSequencer txnSequencer;

    public Producer(TransactionSequencer txnSequencer) {
      this.txnSequencer = txnSequencer;
    }

    @Override
    public void run() {
      System.out.println("producer started");
      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime < TIME_TO_RUN) {
          this.txnSequencer.addTransaction(new TestClientTransaction());
      }
      System.out.println("producer finished");
    }
  }

  private static class Consumer implements Runnable {
    private final TransactionSequencer txnSequencer;

    public Consumer(TransactionSequencer txnSequencer) {
      this.txnSequencer = txnSequencer;
    }

    @Override
    public void run() {
      System.out.println("consumer started");
      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime < TIME_TO_RUN) {
        this.txnSequencer.getNextBatch();
      }
      System.out.println("consumer finished");
    }
  }

  private final class TestTransactionBatchFactory implements TransactionBatchFactory {
    private long idSequence;
    

    public TestTransactionBatchFactory() {
    }
        
    @Override
    public ClientTransactionBatch nextBatch(GroupID groupID) {
      ClientTransactionBatch rv = new TestTransactionBatch(new TxnBatchID(++this.idSequence));
      return rv;
    }

    @Override
    public boolean isFoldingSupported() {
        return folding;
    }
  }

  private final class TestTransactionBatch implements ClientTransactionBatch {

    public final TxnBatchID batchID;
    private int transactions = 0;

    public TestTransactionBatch(TxnBatchID batchID) {
      this.batchID = batchID;
    }

    @Override
    public synchronized boolean isEmpty() {
      return true;
    }

    @Override
    public synchronized int numberOfTxnsBeforeFolding() {
      return transactions;
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
      transactions += 1;
      return new FoldedInfo(createTransactionBuffer());
    }
    
    private TransactionBuffer createTransactionBuffer() {
      TransactionBuffer buffer = Mockito.mock(TransactionBuffer.class);
      Mockito.when(buffer.getTxnCount()).thenReturn(1);
      Mockito.when(buffer.getFoldedTransactionID()).thenReturn(Mockito.mock(TransactionID.class));
      Mockito.doReturn(640000).when(buffer).write(Matchers.any(ClientTransaction.class));
      return buffer;
   }
    
    @Override
    public synchronized TransactionBuffer addSimpleTransaction(ClientTransaction txn) {
      transactions += 1;
      return createTransactionBuffer();
    }
    
    @Override
    public TransactionBuffer removeTransaction(TransactionID txID) {
      return Mockito.mock(TransactionBuffer.class);
    }

    @Override
    public boolean contains(TransactionID txID) {
        return true;
    }

    @Override
    public synchronized Collection addTransactionIDsTo(Collection c) {
      throw new ImplementMe();
    }

    @Override
    public void send() {
      //
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
      throw new ImplementMe();
    }

    @Override
    public String dump() {
      return "TestTransactionBatch";
    }

    @Override
    public int byteSize() {
      return 640000 * transactions;
    }

    @Override
    public String toString() {
        return "TestTransactionBatch{" + "batchID=" + batchID + ", transactions=" + transactions + '}';
    }
    
    

  }

  private class TestLockAccounting extends LockAccounting {

    public TestLockAccounting(AbortableOperationManager abortableOperationManager,
                              RemoteTransactionManagerImpl remoteTxnMgrImpl) {
      super(abortableOperationManager, remoteTxnMgrImpl);
    }

    @Override
    public synchronized void add(TransactionID txID, Collection lockIDs) {
      // do nothing
    }
  }
}
