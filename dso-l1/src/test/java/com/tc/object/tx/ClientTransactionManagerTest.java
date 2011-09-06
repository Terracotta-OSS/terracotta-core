/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.TestClientObjectManager;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.locks.StringLockID;
import com.tc.object.logging.NullRuntimeLogger;
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
    clientTxnFactory = new ClientTransactionFactoryImpl(new NullRuntimeLogger());
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    clientTxnMgr = new ClientTransactionManagerImpl(new ClientIDProviderImpl(new TestChannelIDProvider()), objMgr,
                                                    clientTxnFactory, new MockClientLockManager(), rmtTxnMgr,
                                                    new NullRuntimeLogger(), SampledCounter.NULL_SAMPLED_COUNTER);
  }

  @Override
  public void tearDown() throws Exception {
    if (error.get() != null) { throw new RuntimeException((Throwable) error.get()); }
  }

  public void testCheckWriteAccess() {
    // Test that we get an exception when we have no TXN started
    try {
      clientTxnMgr.checkWriteAccess(new Object());
      fail();
    } catch (UnlockedSharedObjectException usoe) {
      // expected
    }

    // Test that we get an exception when checking while only holding a read lock
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ);
    try {
      clientTxnMgr.checkWriteAccess(new Object());
      fail();
    } catch (UnlockedSharedObjectException roe) {
      // expected
    }
    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.WRITE);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.WRITE);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.SYNCHRONOUS_WRITE);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.SYNCHRONOUS_WRITE);

    clientTxnMgr.begin(new StringLockID("test"), LockLevel.CONCURRENT);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit(new StringLockID("test"), LockLevel.CONCURRENT);
  }

  public void testDoIllegalReadChange() {
    clientTxnMgr.begin(new StringLockID("lock"), LockLevel.READ);

    try {
      clientTxnMgr.fieldChanged(new MockTCObject(new ObjectID(1), new Object()), null, null, null, -1);
      assertFalse(true);
    } catch (UnlockedSharedObjectException e) {
      // expected

      // System.out.println("THIS IS A GOOD THING");
      // e.printStackTrace();
      // System.out.println("THIS IS A GOOD THING");
    }

    clientTxnMgr.commit(new StringLockID("lock"), LockLevel.READ);
  }
}
