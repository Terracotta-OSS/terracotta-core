/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.management.beans.tx.MockClientTxMonitor;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.TestClientObjectManager;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.TestLockManager;
import com.tc.object.lockmanager.impl.ThreadLockManagerImpl;
import com.tc.object.logging.NullRuntimeLogger;

import junit.framework.TestCase;

public class ClientTransactionManagerTest extends TestCase {
  ClientTransactionFactory     clientTxnFactory;
  TestRemoteTransactionManager rmtTxnMgr;
  TestClientObjectManager      objMgr;
  TestLockManager              lockMgr;
  ClientTransactionManagerImpl clientTxnMgr;
  MyObject                     pojo;
  SynchronizedRef              error = new SynchronizedRef(null);

  public void setUp() throws Exception {
    clientTxnFactory = new ClientTransactionFactoryImpl(new NullRuntimeLogger(), new TestChannelIDProvider());
    rmtTxnMgr = new TestRemoteTransactionManager();
    objMgr = new TestClientObjectManager();
    lockMgr = new TestLockManager();
    clientTxnMgr = new ClientTransactionManagerImpl(new TestChannelIDProvider(), objMgr,
                                                    new ThreadLockManagerImpl(lockMgr), clientTxnFactory, rmtTxnMgr,
                                                    new NullRuntimeLogger(), new MockClientTxMonitor());

    pojo = new MyObject();
  }

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
    clientTxnMgr.begin("lock", LockLevel.READ);
    try {
      clientTxnMgr.checkWriteAccess(new Object());
      fail();
    } catch (ReadOnlyException roe) {
      // expected
    }
    clientTxnMgr.commit("lock");

    clientTxnMgr.begin("lock", LockLevel.WRITE);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit("lock");

    clientTxnMgr.begin("lock", LockLevel.CONCURRENT);
    clientTxnMgr.checkWriteAccess(new Object());
    clientTxnMgr.commit("lock");
  }

  public void testDoIllegalReadChange() {
    clientTxnMgr.begin("lock", LockLevel.READ);

    try {
      clientTxnMgr.fieldChanged(new MockTCObject(new ObjectID(1), new Object()), null, null, null, -1);
      assertFalse(true);
    } catch (ReadOnlyException e) {
      // expected

      // System.out.println("THIS IS A GOOD THING");
      // e.printStackTrace();
      // System.out.println("THIS IS A GOOD THING");
    }

    clientTxnMgr.commit("lock");
  }

  private static class MyObject {
    public int count = 0;
  }
}
