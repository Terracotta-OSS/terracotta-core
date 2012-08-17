/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStatElement;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class LockStatisticsJMXTestClient extends ClientBase {
  private MBeanServerConnection      mbsc = null;
  private JMXConnector               jmxc;
  private LockStatisticsMonitorMBean statMBean;
  private Toolkit                    toolkit;
  private ToolkitBarrier             barrier2;

  public LockStatisticsJMXTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit2) throws Exception {
    this.toolkit = toolkit2;
    barrier2 = toolkit.getBarrier("barrierfortwo", 2);
    int index = getBarrierForAllClients().await();

    enableStackTraces(index, 2, 1);
    testTryLock(index, 2);
    testLock(index, 2);

    enableStackTraces(index, 0, 1);

    testServerLockAggregateWaitTime("lock0", index);

    enableStackTraces(index, 1, 1);

    testLockAggregateWaitTime("lock1", index);

    testBasicStatistics("lock2", index);

    enableStackTraces(index, 2, 1);

    testCollectClientStatistics("lock3", index, 1);

  }

  private void connect() throws Exception {
    echo("connecting to jmx server....");
    int jmxPort = getGroupData(0).getJmxPort(0);
    jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    mbsc = jmxc.getMBeanServerConnection();
    echo("obtained mbeanserver connection");
    statMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.LOCK_STATISTICS,
                                                              LockStatisticsMonitorMBean.class, false);
  }

  private void disconnect() throws Exception {
    if (jmxc != null) {
      jmxc.close();
    }
  }

  private void enableStackTraces(int index, int traceDepth, int gatherInterval) throws Exception {
    if (index == 0) {
      connect();
      statMBean.setLockStatisticsConfig(traceDepth, gatherInterval);
      disconnect();
    }

    getBarrierForAllClients().await();
  }

  private void testLock(int index, int traceDepth) throws Exception {
    waitForAllToMoveOn();
    String lockId = "testLock";
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(getLockId(lockId), 1, traceDepth);
      disconnect();
    } else {
      lockMethod(1000, toolkit.getLock(lockId));
      waitForAllToMoveOn();
    }

    waitForAllToMoveOn();
  }

  private void testTryLock(int index, int traceDepth) throws Exception {
    String lockId = "testTryLock";
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(getLockId(lockId), 1, traceDepth);
      disconnect();
    } else {
      tryLockBlock(1000, toolkit.getLock(lockId));
      waitForAllToMoveOn();
    }

    waitForAllToMoveOn();
  }

  private StringLockID getLockId(String lockId) {
    return new StringLockID("_tclock@" + lockId);
  }

  private void testCollectClientStatistics(String lockName, int index, int traceDepth) throws Exception {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(getLockId(lockName), 1, traceDepth);
      disconnect();
    } else {
      ToolkitLock readLock = toolkit.getLock(lockName);
      readLock.lock();
      try {
        readLock.lock();
        try {
          System.out.println("Inside testCollectClientStatistics");
        } finally {
          readLock.unlock();
        }
      } finally {
        readLock.unlock();
      }

      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void assertExpectedTime(long expectedMinTime, long actualTime) {
    // Supposed to be expectedMinTime but changed to expectedMinTime-10
    // This is due to System.currentTimeMillis() which is not that accurate,
    // according to javadoc, the granularity can be in units of tens of milliseconds
    if (Os.isWindows()) {
      // on windows, System.currentTimeMills() only changes every 15-16 millis! Its even worse on windows 95 (~55ms)
      Assert.assertTrue(actualTime >= (expectedMinTime - 200));
    } else {
      Assert.assertTrue(actualTime >= (expectedMinTime - 10));
    }
  }

  private void testServerLockAggregateWaitTime(String lockName, int index) throws Exception {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      LockID lockID = getLockId(lockName);
      long avgWaitTimeInMillis = getServerAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getServerAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        ThreadUtil.reallySleep(2000);
      } finally {
        writeLock.unlock();
      }
      waitForTwoToMoveOn();
      writeLock.lock();
      try {
        ThreadUtil.reallySleep(3000);
      } finally {
        writeLock.unlock();
      }
      waitForAllToMoveOn();
    } else if (index == 2) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      waitForTwoToMoveOn();
      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        ThreadUtil.reallySleep(2000);
      } finally {
        writeLock.unlock();
      }
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testLockAggregateWaitTime(String lockName, int index) throws Exception {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      LockID lockID = getLockId(lockName);
      long avgWaitTimeInMillis = getAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        ThreadUtil.reallySleep(2000);
      } finally {
        writeLock.unlock();
      }
      waitForTwoToMoveOn();
      writeLock.lock();
      try {
        ThreadUtil.reallySleep(3000);
      } finally {
        writeLock.unlock();
      }
      waitForAllToMoveOn();
    } else if (index == 2) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      waitForTwoToMoveOn();
      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        ThreadUtil.reallySleep(2000);
      } finally {
        writeLock.unlock();
      }
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testBasicStatistics(String lockName, int index) throws Exception {
    if (index == 0) {
      LockID lockID = getLockId(lockName);
      connect();
      waitForAllToMoveOn();

      verifyLockRequest(lockID, 1);
      verifyLockAwarded(lockID, 1);
      waitForAllToMoveOn();

      Thread.sleep(1000);
      verifyLockAwarded(lockID, 1);
      waitForTwoToMoveOn();

      waitForAllToMoveOn();
      verifyLockRequest(lockID, 2);
      verifyLockAwarded(lockID, 2);

      waitForAllToMoveOn();

      waitForAllToMoveOn();

      verifyLockHop(lockID, 4);
      waitForAllToMoveOn();
      disconnect();

    } else if (index == 1) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      writeLock.lock();
      try {
        waitForAllToMoveOn();
        waitForAllToMoveOn();

        waitForTwoToMoveOn();
      } finally {
        writeLock.unlock();
      }
      waitForAllToMoveOn();

      waitForAllToMoveOn();
      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        Thread.sleep(1000);
      } finally {
        writeLock.unlock();
      }
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
    } else if (index == 2) {
      ToolkitLock writeLock = toolkit.getLock(lockName);
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      writeLock.lock();
      try {
        waitForAllToMoveOn();

        waitForAllToMoveOn();

        Thread.sleep(1000);
      } finally {
        writeLock.unlock();
      }
      waitForTwoToMoveOn();

      writeLock.lock();
      try {
        waitForTwoToMoveOn();
        waitForAllToMoveOn();
        waitForAllToMoveOn();
      } finally {
        writeLock.unlock();
      }
    }

    waitForAllToMoveOn();
  }

  private void waitForAllToMoveOn() throws Exception {
    getBarrierForAllClients().await();
  }

  private void waitForTwoToMoveOn() throws Exception {
    barrier2.await();
  }

  private void verifyLockRequest(LockID lock, int expectedValue) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockRequested());
  }

  private void verifyLockAwarded(LockID lock, long expectedValue) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockAwarded());
  }

  private long getServerAggregateAverageHeldTime(LockID lock) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getServerStats().getAvgHeldTimeInMillis();
  }

  private long getAggregateAverageHeldTime(LockID lock) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getClientStats().getAvgHeldTimeInMillis();
  }

  private long getServerAggregateAverageWaitTime(LockID lock) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getServerStats().getAvgWaitTimeToAwardInMillis();
  }

  private long getAggregateAverageWaitTime(LockID lock) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getClientStats().getAvgWaitTimeToAwardInMillis();
  }

  private void verifyLockHop(LockID lock, int expectedValue) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockHopRequests());
  }

  private void verifyClientStat(LockID lock, int numOfClientsStackTraces, int traceDepth) throws InterruptedException {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(numOfClientsStackTraces, ls.children().size());
    assertStackTracesDepth(ls.children(), traceDepth);
  }

  private boolean assertStackTracesDepth(Collection traces, int expectedDepthOfStackTraces) {
    if (traces.size() == 0 && expectedDepthOfStackTraces == 0) { return true; }
    if (traces.size() == 0 || expectedDepthOfStackTraces == 0) { return false; }

    LockStatElement lse = (LockStatElement) traces.iterator().next();
    return assertStackTracesDepth(lse.children(), expectedDepthOfStackTraces - 1);
  }

  private static void echo(String msg) {
    System.err.println(msg);
  }

  private LockSpec getLockSpecFor(LockID lock) throws InterruptedException {
    Collection<LockSpec> c = statMBean.getLockSpecs();
    System.err.println("Statistics For: ");
    for (LockSpec ls : c) {
      System.out.println("\t" + ls.getLockID());

      if (ls.getLockID().equals(lock)) { return ls; }
    }
    throw new AssertionError(lock + " cannot be found in the statistics.");
  }

  public void lockMethod(long time, Lock lock) {
    lock.lock();
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    } finally {
      lock.unlock();
    }
  }

  public void tryLockBlock(long time, Lock lock) {
    boolean isLocked = lock.tryLock();
    if (isLocked) {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      } finally {
        lock.unlock();
      }
    }
  }

}
