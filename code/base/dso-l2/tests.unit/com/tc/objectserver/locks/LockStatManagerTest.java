/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.management.L2LockStatsManager;
import com.tc.management.lock.stats.L2LockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockSpec;
import com.tc.net.ClientID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;
import com.tc.object.locks.StringLockID;
import com.tc.object.locks.ThreadID;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.locks.factory.GreedyPolicyFactory;
import com.tc.objectserver.locks.factory.NonGreedyLockPolicyFactory;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class LockStatManagerTest extends TestCase {
  private TestSink             sink;
  private LockManagerImpl      lockManager;
  private L2LockStatsManager   lockStatManager;
  private DSOGlobalServerStats serverStats;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sink = new TestSink();
    resetLockManager();
  }

  private void resetLockManager() {
    resetLockManager(new GreedyPolicyFactory(), false);
  }

  private void resetLockManager(boolean initNullLockManager) {
    resetLockManager(new GreedyPolicyFactory(), initNullLockManager);
  }

  private void resetLockManager(LockFactory factory, boolean initNullLockManager) {
    if (initNullLockManager) {
      lockStatManager = L2LockStatsManager.UNSYNCHRONIZED_LOCK_STATS_MANAGER;
    } else {
      lockStatManager = new L2LockStatisticsManagerImpl();
    }
    if (factory == null) {
      lockManager = new LockManagerImpl(sink, new NullChannelManager());
    } else {
      lockManager = new LockManagerImpl(sink, new NullChannelManager(), factory);
    }
    lockManager.setLockStatisticsEnabled(true, lockStatManager);

    lockManager.start();
    final CounterManager counterManager = new CounterManagerImpl();
    final SampledCounterConfig sampledCounterConfig = new SampledCounterConfig(1, 300, false, 0L);
    final SampledCounter lockRecallCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);
    final SampledCounter lockCounter = (SampledCounter) counterManager.createCounter(sampledCounterConfig);

    this.serverStats = new DSOGlobalServerStatsImpl(null, null, null, null, null, null, null, null, lockRecallCounter,
                                                    null, null, lockCounter);
    lockStatManager.start(new NullChannelManager(), serverStats, ObjectStatsManager.NULL_OBJECT_STATS_MANAGER);
  }

  @Override
  protected void tearDown() throws Exception {
    assertEquals(0, lockManager.getLockCount());
    super.tearDown();
  }

  public void testLockHeldAggregateDurationWithoutGreedy() {
    resetLockManager(new NonGreedyLockPolicyFactory(), false);
    try {
      LockID l1 = new StringLockID("1");
      final ClientID cid1 = new ClientID(1);
      ThreadID s1 = new ThreadID(0);
      final ClientID cid2 = new ClientID(2);
      ThreadID s2 = new ThreadID(1);

      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
      Thread.sleep(5000);
      lockManager.unlock(l1, cid1, s1);
      lockManager.lock(l1, cid2, s2, ServerLockLevel.READ);
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
      LockID l1 = new StringLockID("1");
      final ClientID cid1 = new ClientID(1);
      ThreadID s1 = new ThreadID(0);
      final ClientID cid2 = new ClientID(2);
      ThreadID s2 = new ThreadID(1);

      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
      ThreadUtil.reallySleep(5000);
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);
      lockManager.lock(l1, cid2, s2, ServerLockLevel.READ);
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

  private void assertExpectedTime(final long expectedMinTime, final long actualTime) {
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
      LockID l1 = new StringLockID("1");
      LockID l2 = new StringLockID("2");
      final ClientID cid1 = new ClientID(1);
      ThreadID s1 = new ThreadID(0);

      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
      lockManager.lock(l2, cid1, s1, ServerLockLevel.READ);
      lockManager.unlock(l2, cid1, ThreadID.VM_ID);
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);
      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
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
    try {
      int runningLockRecallCount = 0;
      runningLockRecallCount = verifyLockStatsManagerStatistics(runningLockRecallCount);

      lockStatManager.setLockStatisticsEnabled(false);

      LockID l1 = new StringLockID("1");
      ThreadID s1 = new ThreadID(0);

      final ClientID cid1 = new ClientID(1);

      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
      assertEquals(0, lockStatManager.getNumberOfLockRequested(l1));
      lockManager.unlock(l1, cid1, ThreadID.VM_ID);

      lockStatManager.setLockStatisticsEnabled(true);

      ThreadUtil.reallySleep(1200);
      verifyLockStatsManagerStatistics(runningLockRecallCount);

      resetLockManager(true);

      lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE);
      assertEquals(0, lockStatManager.getNumberOfLockRequested(l1));

      lockManager.lock(l1, new ClientID(2), s1, ServerLockLevel.WRITE);

      lockManager.unlock(l1, cid1, ThreadID.VM_ID);

      // let the samplecounter collect values
      ThreadUtil.reallySleep(2000);

      assertEquals(1, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

      lockManager.unlock(l1, new ClientID(2), ThreadID.VM_ID);

    } catch (Error e) {
      e.printStackTrace();
      throw e;
    }
  }

  private int verifyLockStatsManagerStatistics(int initialLockRecallCount) {
    LockID l1 = new StringLockID("1");
    ThreadID s1 = new ThreadID(0);
    SampledCounter globalLockCounter = serverStats.getGlobalLockCounter();

    final ClientID cid1 = new ClientID(1);
    final ClientID cid2 = new ClientID(2);
    final ClientID cid3 = new ClientID(3);
    final ClientID cid4 = new ClientID(4);

    globalLockCounter.getAndReset();

    long lastSampleTime = System.currentTimeMillis();
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE); // c1 get l1 greedily
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);

    assertEquals(1, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));

    lockManager.lock(l1, cid2, s1, ServerLockLevel.WRITE); // c2 should pend and
    // issue a
    // recall
    assertEquals(2, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(0, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(1, lockStatManager.getNumberOfLockHopRequests(l1));

    // let the samplecounter collect values
    ThreadUtil.reallySleep(2000);
    int recallCount = (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue();
    assertEquals(1 + initialLockRecallCount, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

    lockManager.tryLock(l1, cid3, s1, ServerLockLevel.WRITE, 0);
    assertEquals(3, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));

    globalLockCounter.getAndReset();

    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.unlock(l1, cid1, ThreadID.VM_ID); // it will grant request to c2 greedily
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(1, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.lock(l1, cid1, s1, ServerLockLevel.WRITE); // c1 request again and
    // issue
    // a recall
    assertEquals(4, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(2, lockStatManager.getNumberOfLockHopRequests(l1));

    // let the samplecounter collect values
    ThreadUtil.reallySleep(2000);
    assertEquals(2 + initialLockRecallCount, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

    globalLockCounter.getAndReset();

    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.unlock(l1, cid2, ThreadID.VM_ID); // grant to c1 greedily again
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);
    assertEquals(1, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(2, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.lock(l1, cid3, s1, ServerLockLevel.WRITE); // issues a recall again
    lockManager.lock(l1, cid4, s1, ServerLockLevel.WRITE);
    assertEquals(6, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(3, lockStatManager.getNumberOfLockHopRequests(l1));
    // let the samplecounter collect values
    ThreadUtil.reallySleep(2000);
    assertEquals(3 + initialLockRecallCount, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

    globalLockCounter.getAndReset();

    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.unlock(l1, cid1, ThreadID.VM_ID); // grant to c3 greedily with a lease recall
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(3, lockStatManager.getNumberOfLockReleased(l1));
    assertEquals(4, lockStatManager.getNumberOfLockHopRequests(l1));
    // let the samplecounter collect values
    ThreadUtil.reallySleep(2000);
    assertEquals(4 + initialLockRecallCount, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

    globalLockCounter.getAndReset();

    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.unlock(l1, cid3, ThreadID.VM_ID); // grant to c4 greedily
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(4, lockStatManager.getNumberOfLockReleased(l1));
    lockManager.lock(l1, cid3, s1, ServerLockLevel.WRITE); // issues a recall again
    assertEquals(7, lockStatManager.getNumberOfLockRequested(l1));
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(5, lockStatManager.getNumberOfLockHopRequests(l1));
    // let the samplecounter collect values
    ThreadUtil.reallySleep(2000);
    recallCount = (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue();
    assertEquals(5 + initialLockRecallCount, (int) lockStatManager.getLockRecallMostRecentSample().getCounterValue());

    globalLockCounter.getAndReset();

    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 0);
    lockManager.unlock(l1, cid4, ThreadID.VM_ID); // grant to c3 greedily
    lastSampleTime = sampleAndAssertLockCount(lastSampleTime, globalLockCounter, 1);
    assertEquals(2, lockStatManager.getNumberOfPendingRequests(l1));
    assertEquals(5, lockStatManager.getNumberOfLockReleased(l1));

    lockManager.unlock(l1, cid3, ThreadID.VM_ID);
    assertEquals(6, lockStatManager.getNumberOfLockReleased(l1));
    return recallCount;
  }

  private long sampleAndAssertLockCount(long lastSampleTime, SampledCounter lockCounter, int expected) {
    ThreadUtil.reallySleep(1500);
    Assert.assertEquals(expected, (int) lockCounter.getMostRecentSample().getCounterValue());
    return lastSampleTime;
  }

}
