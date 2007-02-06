/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.object.msg.AcknowledgeTransactionMessageFactory;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.ObjectIDBatchRequestMessageFactory;
import com.tc.object.msg.RequestManagedObjectMessageFactory;
import com.tc.object.msg.RequestRootMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class RemoteTransactionManagerTest extends TestCase {

  private static final TCLogger        logger = TCLogging.getLogger(RemoteTransactionManagerTest.class);

  private RemoteTransactionManagerImpl manager;
  private TestTransactionBatchFactory  batchFactory;
  private SynchronizedInt              number;
  private SynchronizedRef              error;
  private Map                          threads;
  private LinkedQueue                  batchSendQueue;
  private TransactionBatchAccounting   batchAccounting;
  private LockAccounting               lockAccounting;

  public void setUp() throws Exception {
    batchFactory = new TestTransactionBatchFactory();
    batchAccounting = new TransactionBatchAccounting();
    lockAccounting = new LockAccounting();
    manager = new RemoteTransactionManagerImpl(logger, batchFactory, batchAccounting, lockAccounting,
                                               new NullSessionManager(), new MockChannel());
    number = new SynchronizedInt(0);
    error = new SynchronizedRef(null);
    threads = new HashMap();
    batchSendQueue = new LinkedQueue();
  }

  public void tearDown() throws Exception {
    if (error.get() != null) {
      Throwable t = (Throwable) error.get();
      fail(t.getMessage());
    }
    for (Iterator i = batchAccounting.addIncompleteTransactionIDsTo(new LinkedList()).iterator(); i.hasNext();) {
      TransactionID txID = (TransactionID) i.next();
      manager.receivedAcknowledgement(SessionID.NULL_ID, txID);
    }
    batchAccounting.clear();
    batchAccounting.stop();
    manager.clear();
  }

  public void testFlush() throws Exception {
    final LockID lockID1 = new LockID("lock1");
    manager.flush(lockID1);
    TestClientTransaction tx1 = new TestClientTransaction();
    tx1.txID = new TransactionID(1);
    tx1.lockID = lockID1;
    tx1.allLockIDs.add(lockID1);
    tx1.txnType = TxnType.NORMAL;
    manager.commit(tx1);
    final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
    Runnable flusher = new Runnable() {
      public void run() {
        manager.flush(lockID1);
        flushCalls.put(lockID1);
      }
    };

    new Thread(flusher).start();
    // XXX: Figure out how to do this without a timeout.
    int timeout = 5 * 1000;
    System.err.println("About too wait for " + timeout + " ms.");
    assertNull(flushCalls.poll(timeout));

    manager.receivedAcknowledgement(SessionID.NULL_ID, tx1.getTransactionID());
    assertEquals(lockID1, flushCalls.take());

    TestClientTransaction tx2 = tx1;
    tx2.txID = new TransactionID(2);

    // make sure flush falls through if the acknowledgement is received before the flush is called.
    manager.commit(tx1);
    manager.receivedAcknowledgement(SessionID.NULL_ID, tx2.getTransactionID());
    new Thread(flusher).start();
    assertEquals(lockID1, flushCalls.take());
  }

  public void testSendAckedGlobalTransactionIDs() throws Exception {
    assertTrue(batchSendQueue.isEmpty());
    Set acknowledged = new HashSet();
    ClientTransaction ctx = makeTransaction();
    CyclicBarrier barrier = new CyclicBarrier(2);

    callCommitOnThread(ctx, barrier);
    barrier.barrier();
    ThreadUtil.reallySleep(500);
    TestTransactionBatch batch = (TestTransactionBatch) batchFactory.newBatchQueue.poll(1);
    assertNotNull(batch);
    assertSame(batch, batchSendQueue.poll(1));
    assertTrue(batchSendQueue.isEmpty());

    assertEquals(acknowledged, batch.ackedTransactions);

    // fill the current batch with a bunch of transactions
    int count = 10;
    for (int i = 0; i < count; i++) {
      ClientTransaction ctx1 = makeTransaction();
      barrier = new CyclicBarrier(2);
      callCommitOnThread(ctx1, barrier);
      barrier.barrier();
      ThreadUtil.reallySleep(500);
    }

    List batches = new ArrayList();
    TestTransactionBatch batch1;
    while ((batch1 = (TestTransactionBatch) batchSendQueue.poll(3000)) != null) {
      System.err.println("Recd batch " + batch1);
      batches.add(batch1);
    }

    // acknowledge the first transaction
    manager.receivedAcknowledgement(SessionID.NULL_ID, ctx.getTransactionID());
    acknowledged.add(ctx.getTransactionID());

    manager.receivedBatchAcknowledgement(batch.batchID);

    // the batch ack should have sent another batch
    batch = (TestTransactionBatch) batchSendQueue.poll(1);
    assertNotNull(batch);
    assertTrue(batchSendQueue.isEmpty());

    // The set of acked transactions in the batch should be everything we've
    // acknowledged so far.
    assertEquals(acknowledged, batch.ackedTransactions);

    acknowledged.clear();

    ctx = makeTransaction();
    barrier = new CyclicBarrier(2);
    callCommitOnThread(ctx, barrier);
    barrier.barrier();
    ThreadUtil.reallySleep(500);

    // acknowledge the remaining batches so the current batch will get sent.
    for (Iterator i = batches.iterator(); i.hasNext();) {
      batch1 = (TestTransactionBatch) i.next();
      manager.receivedBatchAcknowledgement(batch1.batchID);
    }
    manager.receivedBatchAcknowledgement(batch.batchID);

    batch = (TestTransactionBatch) batchSendQueue.poll(1);
    assertNotNull(batch);
    assertTrue(batchSendQueue.isEmpty());
    // The set of acked transactions should now be empty, since we've already
    // sent them down to the server
    assertEquals(acknowledged, batch.ackedTransactions);
  }

  public void testResendOutstandingBasics() throws Exception {
    System.err.println("Testing testResendOutstandingBasics ...");
    final Set batchTxs = new HashSet();

    final int maxBatchesOutstanding = manager.getMaxOutStandingBatches();
    final List batches = new ArrayList();

    TestTransactionBatch batchN;
    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = (TestTransactionBatch) batchSendQueue.take();
      System.err.println("* Recd " + batchN);
      assertEquals(batchN, getNextNewBatch());
      assertTrue(batchSendQueue.isEmpty());
      batches.add(batchN);
      assertEquals(1, batchN.transactions.size());
    }

    final int num = 5;
    // These txns are not gonna be sent as we already reached the max Batches outstanding count
    makeAndCommitTransactions(batchTxs, num);
    ThreadUtil.reallySleep(2000);
    assertTrue(batchSendQueue.isEmpty());

    // Resend outstanding batches
    restart(manager);

    // Make sure the batches get resent
    for (int i = batches.size(); i > 0; i--) {
      assertTrue(batches.contains(batchSendQueue.take()));
    }
    assertTrue(batchSendQueue.isEmpty());

    // ACK batch 1; next batch (batch 3) will be sent.
    manager.receivedBatchAcknowledgement(((TestTransactionBatch) batches.get(0)).batchID);
    while ((batchN = (TestTransactionBatch) batchSendQueue.poll(3000)) != null) {
      System.err.println("** Recd " + batchN);
      batches.add(batchN);
      getNextNewBatch();
    }

    // Resend outstanding batches
    restart(manager);

    // This time, all batches + batch 3 should get resent
    List sent = (List) drainQueueInto(batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // make some new transactions that should go into next batch
    makeAndCommitTransactions(batchTxs, num);

    // ACK all the transactions in batch1
    Collection batch1Txs = ((TestTransactionBatch) batches.get(0)).addTransactionIDsTo(new HashSet());
    for (Iterator i = batch1Txs.iterator(); i.hasNext();) {
      TransactionID txnId = (TransactionID) i.next();
      batchTxs.remove(txnId);
      manager.receivedAcknowledgement(SessionID.NULL_ID, txnId);
    }
    batches.remove(0);

    // resend
    restart(manager);

    // This time, batches except batch 1 should get resent
    sent = (List) drainQueueInto(batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // ACK all other batches
    for (Iterator i = batches.iterator(); i.hasNext();) {
      batchN = (TestTransactionBatch) i.next();
      manager.receivedBatchAcknowledgement(batchN.batchID);
    }

    while ((batchN = (TestTransactionBatch) batchSendQueue.poll(3000)) != null) {
      System.err.println("*** Recd " + batchN);
      batches.add(batchN);
      getNextNewBatch();
    }

    // resend
    restart(manager);

    // This time, batches except batch 1 should get resent
    sent = (List) drainQueueInto(batchSendQueue, new LinkedList());
    assertEquals(batches.size(), sent.size());
    assertTrue(sent.containsAll(batches));

    // now make sure that the manager re-sends an outstanding batch until all of
    // its transactions have been acked.
    while (batches.size() > 0) {
      Collection batchNTxs = ((TestTransactionBatch) batches.get(0)).addTransactionIDsTo(new HashSet());
      for (Iterator i = batchNTxs.iterator(); i.hasNext();) {
        TransactionID txnId = (TransactionID) i.next();
        batchTxs.remove(txnId);
        manager.receivedAcknowledgement(SessionID.NULL_ID, txnId);
        restart(manager);
        sent = (List) drainQueueInto(batchSendQueue, new LinkedList());
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

  private void restart(RemoteTransactionManager manager2) {
    manager2.pause();
    manager2.starting();
    manager2.resendOutstandingAndUnpause();

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

    final int maxBatchesOutstanding = manager.getMaxOutStandingBatches();
    TestTransactionBatch batchN;
    final Set batchTxs = new HashSet();
    final List batches = new ArrayList();

    for (int i = 0; i < maxBatchesOutstanding; i++) {
      makeAndCommitTransactions(batchTxs, 1);
      batchN = (TestTransactionBatch) batchSendQueue.take();
      System.err.println("* Recd " + batchN);
      assertEquals(batchN, getNextNewBatch());
      assertTrue(batchSendQueue.isEmpty());
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
    assertTrue(batchSendQueue.isEmpty());

    // Make sure the rest transactions get into the second batch
    TestTransactionBatch batch2 = getNextNewBatch();
    Collection txnsInBatch = drainQueueInto(batch2.addTxQueue, new HashSet());
    assertTrue(batch2Txs.size() == txnsInBatch.size());
    txnsInBatch.removeAll(batch2Txs);
    assertTrue(txnsInBatch.size() == 0);
    assertTrue(batch2.addTxQueue.isEmpty());

    TestTransactionBatch batch1 = ((TestTransactionBatch) batches.remove(0));

    // ACK one of the batch (triggers send of next batch)
    manager.receivedBatchAcknowledgement(batch1.batchID);
    // make sure that the batch sent is what we expected.
    assertSame(batch2, batchSendQueue.take());

    TestTransactionBatch batch3 = getNextNewBatch();

    // ACK another batch (no more TXNs to send this time)
    assertTrue(batchSendQueue.isEmpty());
    manager.receivedBatchAcknowledgement(batch2.batchID);
    assertTrue(batchSendQueue.isEmpty());
    for (Iterator i = batches.iterator(); i.hasNext();) {
      TestTransactionBatch b = (TestTransactionBatch) i.next();
      manager.receivedBatchAcknowledgement(b.batchID);
      assertTrue(batchSendQueue.isEmpty());
    }

    for (Iterator i = batchTxs.iterator(); i.hasNext();) {
      ClientTransaction txn = (ClientTransaction) i.next();
      manager.receivedAcknowledgement(SessionID.NULL_ID, txn.getTransactionID());
      assertTrue(batchSendQueue.isEmpty());
    }

    // There should still be no batch to send.
    assertTrue(batchSendQueue.isEmpty());
    assertTrue(drainQueueInto(batch3.addTxQueue, new LinkedList()).isEmpty());
  }

  private Collection drainQueueInto(LinkedQueue queue, Collection dest) throws InterruptedException {
    while (!queue.isEmpty()) {
      dest.add(queue.take());
    }
    return dest;
  }

  private TestTransactionBatch getNextNewBatch() throws InterruptedException {
    TestTransactionBatch rv = (TestTransactionBatch) batchFactory.newBatchQueue.take();
    return rv;
  }

  private synchronized void callCommitOnThread(final ClientTransaction txn, final CyclicBarrier barrier) {
    TransactionID txnID = txn.getTransactionID();

    Thread t = new Thread("Commit for txn #" + txnID.toLong()) {
      public void run() {
        try {
          barrier.barrier();
          manager.commit(txn);
        } catch (Throwable th) {
          th.printStackTrace();
          error.set(th);
        }
      }
    };

    threads.put(txnID, t);
    t.start();
  }

  private ClientTransaction makeTransaction() {
    int num = number.increment();
    LockID lid = new LockID("lock" + num);
    TransactionContext tc = new TransactionContext(lid, TxnType.NORMAL, new LockID[] { lid });
    ClientTransaction txn = new ClientTransactionImpl(new TransactionID(num), new NullRuntimeLogger(), null);
    txn.setTransactionContext(tc);
    txn.fieldChanged(new MockTCObject(new ObjectID(num), this), "class", "class.field", new ObjectID(num), -1);
    return txn;
  }

  private final class TestTransactionBatch implements ClientTransactionBatch {

    public final Set         ackedTransactions = new HashSet();

    public final TxnBatchID  batchID;

    public final LinkedQueue addTxQueue        = new LinkedQueue();
    private final LinkedList transactions      = new LinkedList();

    public TestTransactionBatch(TxnBatchID batchID) {
      this.batchID = batchID;
    }

    public String toString() {
      return "TestTransactionBatch[" + batchID + "] = Txn [ " + transactions + " ]";
    }

    public synchronized boolean isEmpty() {
      return transactions.isEmpty();
    }

    public int numberOfTxns() {
      return transactions.size();
    }

    public boolean isNull() {
      return false;
    }

    public synchronized void addTransaction(ClientTransaction txn) {
      try {
        addTxQueue.put(txn);
        transactions.add(txn);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    public void removeTransaction(TransactionID txID) {
      return;
    }

    public Collection addTransactionIDsTo(Collection c) {
      for (Iterator i = transactions.iterator(); i.hasNext();) {
        ClientTransaction txn = (ClientTransaction) i.next();
        c.add(txn.getTransactionID());
      }
      return c;
    }

    public void send() {
      try {
        batchSendQueue.put(this);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
    }

    public TCByteBuffer[] getData() {
      return null;
    }

    public void addAcknowledgedTransactionIDs(Collection acknowledged) {
      ackedTransactions.addAll(acknowledged);
    }

    public Collection getAcknowledgedTransactionIDs() {
      throw new ImplementMe();
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

    public Collection addTransactionSequenceIDsTo(Collection sequenceIDs) {
      for (Iterator i = transactions.iterator(); i.hasNext();) {
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

    public synchronized ClientTransactionBatch nextBatch() {
      ClientTransactionBatch rv = new TestTransactionBatch(new TxnBatchID(++idSequence));
      try {
        newBatchQueue.put(rv);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      return rv;
    }
  }

  private static class MockChannel implements DSOClientMessageChannel {

    public void addClassMapping(TCMessageType messageType, Class messageClass) {
      throw new ImplementMe();
    }

    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();
    }

    public ClientMessageChannel channel() {
      throw new ImplementMe();
    }

    public void close() {
      throw new ImplementMe();
    }

    public AcknowledgeTransactionMessageFactory getAcknowledgeTransactionMessageFactory() {
      throw new ImplementMe();
    }

    public ChannelIDProvider getChannelIDProvider() {
      throw new ImplementMe();
    }

    public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
      throw new ImplementMe();
    }

    public CommitTransactionMessageFactory getCommitTransactionMessageFactory() {
      throw new ImplementMe();
    }

    public LockRequestMessageFactory getLockRequestMessageFactory() {
      throw new ImplementMe();
    }

    public ObjectIDBatchRequestMessageFactory getObjectIDBatchRequestMessageFactory() {
      throw new ImplementMe();
    }

    public RequestManagedObjectMessageFactory getRequestManagedObjectMessageFactory() {
      throw new ImplementMe();
    }

    public RequestRootMessageFactory getRequestRootMessageFactory() {
      throw new ImplementMe();
    }

    public boolean isConnected() {
      throw new ImplementMe();
    }

    public void open() {
      throw new ImplementMe();
    }

    public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
      throw new ImplementMe();
    }
    //
  }

}
