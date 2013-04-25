/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.junit.Assert;
import org.mockito.Mockito;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.abortable.NullAbortableOperationManager;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.TestClientObjectManager;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.StringLockID;
import com.tc.stats.counter.sampled.SampledCounter;

import junit.framework.TestCase;

public class ClientTransactionManagerTest extends TestCase {
  ClientTransactionFactory     clientTxnFactory;
  TestRemoteTransactionManager rmtTxnMgr;
  TestClientObjectManager      objMgr;
  ClientTransactionManagerImpl clientTxnMgr;
  SynchronizedRef              error = new SynchronizedRef(null);

  @Override
  public void setUp() throws Exception {
    clientTxnFactory = new ClientTransactionFactoryImpl();
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    clientTxnMgr = new ClientTransactionManagerImpl(new ClientIDProviderImpl(new TestChannelIDProvider()), objMgr,
                                                    clientTxnFactory, new MockClientLockManager(), rmtTxnMgr,
                                                    SampledCounter.NULL_SAMPLED_COUNTER, null,
                                                    new NullAbortableOperationManager());
  }

  @Override
  public void tearDown() throws Exception {
    if (error.get() != null) { throw new RuntimeException((Throwable) error.get()); }
  }

  public void testCheckWriteAccess() throws Exception {
    // Test that we get an exception when we have no TXN started
    try {
      clientTxnMgr.checkWriteAccess(new Object());
      fail();
    } catch (UnlockedSharedObjectException usoe) {
      // expected
    }

    // Test that we get an exception when checking while only holding a read lock
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ, false);
    try {
      clientTxnMgr.checkWriteAccess(new Object());
      fail();
    } catch (UnlockedSharedObjectException roe) {
      // expected
    }
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ, false, null);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.WRITE, false);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.WRITE, false, null);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.SYNCHRONOUS_WRITE, false);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.SYNCHRONOUS_WRITE, false, null);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.CONCURRENT, false);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.CONCURRENT, false, null);
  }

  public void testDoIllegalReadChange() throws Exception {
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ, false);

    try {
      clientTxnMgr.fieldChanged(new MockTCObject(new ObjectID(1), new Object()), null, null, null, -1);
      assertFalse(true);
    } catch (UnlockedSharedObjectException e) {
      // expected

      // System.out.println("THIS IS A GOOD THING");
      // e.printStackTrace();
      // System.out.println("THIS IS A GOOD THING");
    }

    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ, false, null);
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
    clientTxnMgr.logicalInvoke(new MockTCObject(new ObjectID(1), new Object(), false, true), 1, "test", new Object[0]);
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
}
