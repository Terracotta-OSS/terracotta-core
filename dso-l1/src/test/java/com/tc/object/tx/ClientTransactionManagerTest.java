/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.junit.Assert;
import org.mockito.Mockito;

import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.abortable.AbortedOperationException;
import com.tc.exception.PlatformRejoinException;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.LogicalOperation;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.TestClientObjectManager;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.StringLockID;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.util.concurrent.ThreadUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class ClientTransactionManagerTest extends TestCase {
  ClientTransactionFactory              clientTxnFactory;
  TestRemoteTransactionManager          rmtTxnMgr;
  TestClientObjectManager               objMgr;
  ClientTransactionManagerImpl          clientTxnMgr;
  AtomicReference<Throwable>            error = new AtomicReference<Throwable>(null);
  private AbortableOperationManagerImpl abortableOperationManager;
  private LogicalChangeID               logicalChangeID;

  @Override
  public void setUp() throws Exception {
    clientTxnFactory = new ClientTransactionFactoryImpl();
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    abortableOperationManager = new AbortableOperationManagerImpl();
    logicalChangeID = new LogicalChangeID(1);
    clientTxnMgr = new ClientTransactionManagerImpl(new ClientIDProviderImpl(new TestChannelIDProvider()), objMgr,
                                                    clientTxnFactory, new MockClientLockManager(), rmtTxnMgr,
                                                    SampledCounter.NULL_SAMPLED_COUNTER, null,
        abortableOperationManager) {
      @Override
      LogicalChangeID getNextLogicalChangeId() {
        return logicalChangeID;
      }
    };
  }

  @Override
  public void tearDown() throws Exception {
    if (error.get() != null) { throw new RuntimeException(error.get()); }
  }

  public void testInvalidAtomicSequence() throws Exception {
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
    try {
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      // expected
    } finally {
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, true, null);
    }
    Assert.assertNull(clientTxnMgr.getCurrentTransaction());
    // test state reset for atomic
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, null);

    // try commit atomically when current txn is not atomic
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    try {
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, true, null);
    } catch (IllegalStateException e) {
      // expected
    } finally {
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, null);
    }

  }

  public void testOverlappingAtomicAndNonAtomicTxn() throws Exception {
    OnCommitCallable commitCallable = Mockito.mock(OnCommitCallable.class);
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    doSomeChange();
    Assert.assertFalse(clientTxnMgr.getCurrentTransaction().isAtomic());
    // begin atomic txn
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
    doSomeChange();
    // commit nonatomic first
    Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);
    // txn should still be atomic
    Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
    Mockito.verify(commitCallable, Mockito.never()).call();
    Assert.assertEquals(0, rmtTxnMgr.getTxnCount());
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, true, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(2)).call();
    // check atomicity of changes
    Assert.assertEquals(1, rmtTxnMgr.getTxnCount());

    // now begin atomic first
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
    doSomeChange();
    Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    doSomeChange();
    Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
    // commit atomic first
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, true, commitCallable);
    doSomeChange();
    Assert.assertFalse(clientTxnMgr.getCurrentTransaction().isAtomic());
    Mockito.verify(commitCallable, Mockito.times(3)).call();
    Assert.assertEquals(2, rmtTxnMgr.getTxnCount());
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(4)).call();
    Assert.assertEquals(3, rmtTxnMgr.getTxnCount());
  }

  public void doSomeChange() {
    clientTxnMgr.logicalInvoke(new MockTCObject(new ObjectID(1), new Object()), LogicalOperation.ADD, new Object[0]);
  }

  public void testAtomicTxn() throws Exception {
    OnCommitCallable commitCallable = Mockito.mock(OnCommitCallable.class);
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
    try {
      doSomeChange();
      clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
      doSomeChange();
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);

      clientTxnMgr.begin(new StringLockID("lock"), LockLevel.SYNCHRONOUS_WRITE, false);
      doSomeChange();
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.SYNCHRONOUS_WRITE, false, commitCallable);
      // callables for Write/Sync_WRITE not called until atomic commit
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      Mockito.verify(commitCallable, Mockito.never()).call();
      clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ, false);
      doSomeChange();
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ, false, commitCallable);
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      Mockito.verify(commitCallable, Mockito.times(1)).call();
      clientTxnMgr.begin(new StringLockID("lock"), LockLevel.CONCURRENT, false);
      doSomeChange();
      Assert.assertTrue(clientTxnMgr.getCurrentTransaction().isAtomic());
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.CONCURRENT, false, commitCallable);
      Mockito.verify(commitCallable, Mockito.times(2)).call();

    } finally {
      clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, true, commitCallable);
      // called 2 for nested write locks and 1 for atomic write lock
    }
    Mockito.verify(commitCallable, Mockito.times(5)).call();
    // check atomicity of all above changes
    Assert.assertEquals(1, rmtTxnMgr.getTxnCount());
    Assert.assertNull(clientTxnMgr.getCurrentTransaction());

  }

  public void testNormalTxn() throws Exception {
    OnCommitCallable commitCallable = Mockito.mock(OnCommitCallable.class);
    // test outside atomic txn
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    doSomeChange();
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(1)).call();
    Assert.assertEquals(1, rmtTxnMgr.getTxnCount());
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.SYNCHRONOUS_WRITE, false);
    doSomeChange();
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.SYNCHRONOUS_WRITE, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(2)).call();
    Assert.assertEquals(2, rmtTxnMgr.getTxnCount());
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ, false);
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(3)).call();
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ, false);
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(4)).call();
    // test nested commit
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    doSomeChange();
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);
    Assert.assertEquals(3, rmtTxnMgr.getTxnCount());
    Mockito.verify(commitCallable, Mockito.times(5)).call();
    doSomeChange();
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, commitCallable);
    Mockito.verify(commitCallable, Mockito.times(6)).call();
    Assert.assertEquals(4, rmtTxnMgr.getTxnCount());
  }

  public void testLogicalInvokeResultWithoutTransaction() throws AbortedOperationException {
    try {
      clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()), LogicalOperation.ADD,
                                           new Object[0]);
      Assert.fail();
    } catch (UnlockedSharedObjectException e) {
      // expected
    }
  }

  public void testLogicalInvokeWithResultNotSupportedForAtomicTransaction() throws AbortedOperationException {
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, true);
    try {
      clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()), LogicalOperation.ADD,
                                           new Object[0]);
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void testLogicalInvokeWithResultReturnsOnAbort() throws Exception {
    abortableOperationManager.begin();
    final Thread executorThread = Thread.currentThread();
    Thread thread = new Thread("AbortAfterCommitThread") {
      @Override
      public void run() {
        while (rmtTxnMgr.getTxnCount() == 0) {
          ThreadUtil.reallySleep(100);
        }
        abortableOperationManager.abort(executorThread);
      }
    };
    thread.start();

    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    try {
      clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()), LogicalOperation.ADD,
                                           new Object[0]);
      Assert.fail();
    } catch (AbortedOperationException e) {
      // expected, clear the interrupted status
      Thread.interrupted();
    }
    Assert.assertEquals(0, clientTxnMgr.getLogicalChangeCallbacks().size());
    thread.join();

    Map<LogicalChangeID, LogicalChangeResult> results = new HashMap<LogicalChangeID, LogicalChangeResult>();
    results.put(logicalChangeID, LogicalChangeResult.SUCCESS);
    clientTxnMgr.receivedLogicalChangeResult(results); // Should not throw exception if operation was aborted
  }

  public void testLogicalInvokeWithResultthrowsRejoinExceptionOnCleanup() throws Exception {
    Thread thread = new Thread("CleanupAfterCommitThread") {
      @Override
      public void run() {
        while (rmtTxnMgr.getTxnCount() == 0) {
          ThreadUtil.reallySleep(100);
        }
        clientTxnMgr.cleanup();
      }
    };
    thread.start();

    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    try {
      clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()), LogicalOperation.ADD,
                                           new Object[0]);
      Assert.fail();
    } catch (PlatformRejoinException e) {
      // expected, clear the interrupted status
      Thread.interrupted();
    }
    Assert.assertEquals(0, clientTxnMgr.getLogicalChangeCallbacks().size());
    thread.join();

  }

  public void testLogicalInvokeReturnsTrueOnSuccess() throws Exception {
    Thread thread = new Thread("SimulateLogicalInvokeResultAfterCommitThread") {
      @Override
      public void run() {
        while (rmtTxnMgr.getTxnCount() == 0) {
          ThreadUtil.reallySleep(100);
        }
        Map<LogicalChangeID, LogicalChangeResult> results = new HashMap<LogicalChangeID, LogicalChangeResult>();
        results.put(logicalChangeID, LogicalChangeResult.SUCCESS);
        clientTxnMgr.receivedLogicalChangeResult(results);
      }
    };
    thread.start();
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    Assert
.assertTrue(clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()),
                                                         LogicalOperation.ADD, new Object[0]));

    Assert.assertEquals(0, clientTxnMgr.getLogicalChangeCallbacks().size());
    thread.join();
  }

  public void testLogicalInvokeReturnsFalseOnFailure() throws Exception {
    Thread thread = new Thread("SimulateLogicalInvokeResultAfterCommitThread") {
      @Override
      public void run() {
        while (rmtTxnMgr.getTxnCount() == 0) {
          ThreadUtil.reallySleep(100);
        }
        Map<LogicalChangeID,LogicalChangeResult> results = new HashMap<LogicalChangeID,LogicalChangeResult>();
        results.put(logicalChangeID, LogicalChangeResult.FAILURE);
        clientTxnMgr.receivedLogicalChangeResult(results);
      }
    };
    thread.start();
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);

    Assert
.assertFalse(clientTxnMgr.logicalInvokeWithResult(new MockTCObject(new ObjectID(1), new Object()),
                                                          LogicalOperation.ADD, new Object[0]));

    Assert.assertEquals(0, clientTxnMgr.getLogicalChangeCallbacks().size());
    thread.join();
  }

  public void testNotifyEmptytransaction() throws Exception {
    final AtomicBoolean txnCompletionNotified = new AtomicBoolean(false);
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.WRITE, false);
    // empty transaction and add a listener
    clientTxnMgr.getCurrentTransaction().addTransactionCompleteListener(new TransactionCompleteListener() {
      
      @Override
      public void transactionComplete(TransactionID txnID) {
        txnCompletionNotified.set(true);
      }
      
      @Override
      public void transactionAborted(TransactionID txnID) {
        //
      }
    });
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.WRITE, false, null);
    Assert.assertTrue(txnCompletionNotified.get());
  }

}
