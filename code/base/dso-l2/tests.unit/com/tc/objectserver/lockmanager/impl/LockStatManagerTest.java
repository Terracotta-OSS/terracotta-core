/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockSpec;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.TimerSpec;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.lockmanager.api.NullChannelManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class LockStatManagerTest extends TestCase {
  private TestSink           sink;
  private LockManagerImpl    lockManager;
  private L2LockStatsManager lockStatManager;

  protected void setUp() throws Exception {
    super.setUp();
    resetLockManager();
    sink = new TestSink();
  }

  private void resetLockManager() {
    if (lockManager != null) {
      try {
        lockManager.stop();
      } catch (InterruptedException e) {
        fail();
      }
    }

    lockStatManager = new L2LockStatisticsManagerImpl();
    lockManager = new LockManagerImpl(new NullChannelManager(), lockStatManager);
    lockManager.setLockPolicy(LockManagerImpl.GREEDY_LOCK_POLICY);
    lockManager.start();
    lockStatManager.start(new NullChannelManager());
  }

  protected void tearDown() throws Exception {
    assertEquals(0, lockManager.getLockCount());
    assertEquals(0, lockManager.getThreadContextCount());
    super.tearDown();
  }

  public void testLockHeldAggregateDurationWithoutGreedy() {
    lockManager.setLockPolicy(LockManagerImpl.ALTRUISTIC_LOCK_POLICY);
    try {
      LockID l1 = new LockID("1");
      final ClientID cid1 = new ClientID(new ChannelID(1));
      ThreadID s1 = new ThreadID(0);
      final ClientID cid2 = new ClientID(new ChannelID(2));
      ThreadID s2 = new ThreadID(1);

      lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink);
      Thread.sleep(5000);
      lockManager.unlock(l1, cid1, s1);
      lockManager.requestLock(l1, cid2, s2, LockLevel.READ, String.class.getName(), sink);
      Thread.sleep(3000);
      lockManager.unlock(l1, cid2, s2);
      Collection c = lockStatManager.getLockSpecs();
      Assert.assertEquals(1, c.size());
      Iterator i = c.iterator();
      LockSpec lockStatisticsInfo = (LockSpec) i.next();
      long avgHeldTimeInMillis = lockStatisticsInfo.getServerStats().getAvgHeldTimeInMillis();
      long avgWaitTimeInMillis = lockStatisticsInfo.getServerStats().getAvgWaitTimeToAwardInMillis();
      System.out.println("Average held time in millis: " + avgHeldTimeInMillis);
      System.out.println("Average wait time in millis: " + avgWaitTimeInMillis);
      assertExpectedTime(4000, avgHeldTimeInMillis);
    } catch (InterruptedException e) {
      // ignore
    } finally {
      resetLockManager();
    }
  }

  public void testLockHeldAggregateDuration() {
    try {
      LockID l1 = new LockID("1");
      final ClientID cid1 = new ClientID(new ChannelID(1));
      ThreadID s1 = new ThreadID(0);
      final ClientID cid2 = new ClientID(new ChannelID(2));
      ThreadID s2 = new ThreadID(1);

      lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink);
      ThreadUtil.reallySleep(5000);
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);
      lockManager.requestLock(l1, cid2, s2, LockLevel.READ, String.class.getName(), sink);
      ThreadUtil.reallySleep(3000);
      lockManager.unlock(l1, cid2, ThreadID.VM_ID);
      Collection c = lockStatManager.getLockSpecs();
      Assert.assertEquals(1, c.size());
      Iterator i = c.iterator();
      LockSpec lockStatisticsInfo = (LockSpec) i.next();
      long avgHeldTimeInMillis = lockStatisticsInfo.getServerStats().getAvgHeldTimeInMillis();
      long avgWaitTimeInMillis = lockStatisticsInfo.getServerStats().getAvgWaitTimeToAwardInMillis();
      System.out.println("Average held time in millis: " + avgHeldTimeInMillis);
      System.out.println("Average wait time in millis: " + avgWaitTimeInMillis);
      assertExpectedTime(4000, avgHeldTimeInMillis);
    } finally {
      resetLockManager();
    }
  }

  private void assertExpectedTime(long expectedMinTime, long actualTime) {
    // Supposed to be expectedMinTime but changed to expectedMinTime-10
    // This is due to System.currentTimeMillis() which is not that accurate,
    // according to javadoc, the granularity can be in units of tens of milliseconds
    if (Os.isWindows()) {
      // on windows, System.currentTimeMills() only changes every 15-16 millis! It’s even worse on windows 95 (~55ms)
      Assert.assertTrue(actualTime >= (expectedMinTime - 200));
    } else {
      Assert.assertTrue(actualTime >= (expectedMinTime - 10));
    }
  }

  public void testNestedDepth() {
    try {
      LockID l1 = new LockID("1");
      LockID l2 = new LockID("2");
      final ClientID cid1 = new ClientID(new ChannelID(1));
      ThreadID s1 = new ThreadID(0);

      lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink);
      lockManager.requestLock(l2, cid1, s1, LockLevel.READ, String.class.getName(), sink);
      lockManager.unlock(l2, cid1, ThreadID.VM_ID);
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);
      lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink);
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);
      Collection c = lockStatManager.getLockSpecs();
      Iterator i = c.iterator();
      LockSpec lockStatisticsInfo = (LockSpec) i.next();
      long avgNestedLockDepth = lockStatisticsInfo.getServerStats().getAvgNestedLockDepth();
      Assert.assertEquals(2, avgNestedLockDepth);
    } finally {
      resetLockManager();
    }
  }

  public void testLockStatsManager() {
    veriyLockStatsManagerStatistics();

    lockStatManager.setLockStatisticsEnabled(false);

    LockID l1 = new LockID("1");
    ThreadID s1 = new ThreadID(0);

    final ClientID cid1 = new ClientID(new ChannelID(1));

    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink);
    assertEquals(0, lockStatManager.getNumberOfLockRequested(l1));
    lockManager.unlock(l1, cid1, ThreadID.VM_ID);

    lockStatManager.setLockStatisticsEnabled(true);

    veriyLockStatsManagerStatistics();
  }

  private void veriyLockStatsManagerStatistics() {
    LockID l1 = new LockID("1");
    ThreadID s1 = new ThreadID(0);

    final ClientID cid1 = new ClientID(new ChannelID(1));
    final ClientID cid2 = new ClientID(new ChannelID(2));
    final ClientID cid3 = new ClientID(new ChannelID(3));
    final ClientID cid4 = new ClientID(new ChannelID(4));

    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink); // c1 get l1 greedily
    assertEquals(1, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.requestLock(l1, cid2, s1, LockLevel.WRITE, String.class.getName(), sink); // c2 should pend and issue a
    // recall
    assertEquals(2, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(1, lockStatManager.getNumberOfLockHopRequests(l1));

    lockManager.tryRequestLock(l1, cid3, s1, LockLevel.WRITE, String.class.getName(), new TimerSpec(0, 0), sink);
    assertEquals(3, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.unlock(l1, cid1, ThreadID.VM_ID); // it will grant request to c2 greedily
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(1, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.requestLock(l1, cid1, s1, LockLevel.WRITE, String.class.getName(), sink); // c1 request again and issue
    // a recall
    assertEquals(4, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(2, lockStatManager.getNumberOfLockHopRequests(l1));

    lockManager.unlock(l1, cid2, ThreadID.VM_ID); // grant to c1 greedily again
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(2, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.requestLock(l1, cid3, s1, LockLevel.WRITE, String.class.getName(), sink); // issues a recall again
    lockManager.requestLock(l1, cid4, s1, LockLevel.WRITE, String.class.getName(), sink);
    assertEquals(6, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(3, lockStatManager.getNumberOfLockHopRequests(l1));

    lockManager.unlock(l1, cid1, ThreadID.VM_ID); // grant to c3 greedily with a lease recall
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(3, lockStatManager.getNumberOfLockReleased(l1));
    assertEquals(4, lockStatManager.getNumberOfLockHopRequests(l1));

    lockManager.unlock(l1, cid3, ThreadID.VM_ID); // grant to c4 greedily
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(4, lockStatManager.getNumberOfLockReleased(l1));
    lockManager.requestLock(l1, cid3, s1, LockLevel.WRITE, String.class.getName(), sink); // issues a recall again
    assertEquals(7, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(5, lockStatManager.getNumberOfLockHopRequests(l1));

    lockManager.unlock(l1, cid4, ThreadID.VM_ID); // grant to c3 greedily
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(5, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.unlock(l1, cid3, ThreadID.VM_ID);
    assertEquals(6, lockStatManager.getNumberOfLockReleased(l1));
  }

}
