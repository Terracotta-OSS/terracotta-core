/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStatElement;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.locks.LockID;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Collection;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class LockStatisticsJMXTestApp extends AbstractTransparentApp {
  private static final String NAMED_LOCK_NAME = "nameLock";

  public static final String         CONFIG_FILE      = "config-file";
  public static final String         PORT_NUMBER      = "port-number";
  public static final String         HOST_NAME        = "host-name";
  public static final String         JMX_PORT         = "jmx-port";

  private final ApplicationConfig    config;

  private final int                  initialNodeCount = getParticipantCount();
  private final CyclicBarrier        barrier          = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier        barrier2         = new CyclicBarrier(2);
  private final Object               sharedRoot       = new TestClass();

  private MBeanServerConnection      mbsc             = null;
  private JMXConnector               jmxc;
  private LockStatisticsMonitorMBean statMBean;

  public LockStatisticsJMXTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = LockStatisticsJMXTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression, methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    methodExpression = "* " + testClass + "$*.syncMethod(..)";
    config.addWriteAutolock(methodExpression, methodExpression);
    methodExpression = "* " + testClass + "$*.syncBlock(..)";
    config.addWriteAutolock(methodExpression, methodExpression);
    methodExpression = "* " + testClass + "$*.nameLockMethod(..)";
    LockDefinition ld = new LockDefinitionImpl(NAMED_LOCK_NAME, ConfigLockLevel.WRITE, methodExpression);
    ld.commit();
    config.addLock(methodExpression, ld);

    // roots
    spec.addRoot("barrier", "barrier");
    spec.addRoot("barrier2", "barrier2");
    spec.addRoot("sharedRoot", "sharedRoot");
  }

  public void run() {
    try {
      int index = barrier.await();

      enableStackTraces(index, 2, 1);
      testTryAutoLock(index, 2);
      testAutoLock(index, 2);
      testNameLock(index, 2);
      
      enableStackTraces(index, 0, 1);

      testServerLockAggregateWaitTime("lock0", index);

      enableStackTraces(index, 1, 1);

      testLockAggregateWaitTime("lock1", index);

      testBasicStatistics("lock2", index);

      enableStackTraces(index, 2, 1);

      testCollectClientStatistics("lock3", index, 1);

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void connect() throws Exception {
    echo("connecting to jmx server....");
    jmxc = new JMXConnectorProxy("localhost", Integer.parseInt(config.getAttribute(JMX_PORT)));
    mbsc = jmxc.getMBeanServerConnection();
    echo("obtained mbeanserver connection");
    statMBean = (LockStatisticsMonitorMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbsc, L2MBeanNames.LOCK_STATISTICS, LockStatisticsMonitorMBean.class, false);
  }

  private void disconnect() throws Exception {
    if (jmxc != null) {
      jmxc.close();
    }
  }

  private void enableStackTraces(int index, int traceDepth, int gatherInterval) throws Throwable {
    if (index == 0) {
      connect();
      statMBean.setLockStatisticsConfig(traceDepth, gatherInterval);
      disconnect();
    }

    barrier.await();
  }
  
  private void testNameLock(int index, int traceDepth) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(ManagerUtil.getManager().generateLockIdentifier(ByteCodeUtil.generateNamedLockName(NAMED_LOCK_NAME)), 1, traceDepth);
      disconnect();
    } else {
      TestClass tc = (TestClass)sharedRoot;
      tc.nameLockMethod(1000);
      waitForAllToMoveOn();
    }
    
    waitForAllToMoveOn();
  }

  private void testAutoLock(int index, int traceDepth) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      LockID lock = ManagerUtil.getManager().generateLockIdentifier(sharedRoot);
      verifyClientStat(lock, TestClass.class.getName(), 2, traceDepth);
      disconnect();
    } else {
      TestClass tc = (TestClass)sharedRoot;
      tc.syncMethod(1000);
      tc.syncBlock(2000);
      waitForAllToMoveOn();
    }
    
    waitForAllToMoveOn();
  }
  
  private void testTryAutoLock(int index, int traceDepth) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      TestClass tc = (TestClass)sharedRoot;
      LockID lock = ManagerUtil.getManager().generateLockIdentifier(tc.getTryLock());
      verifyClientStat(lock, ReentrantLock.class.getName(), 1, traceDepth);
      disconnect();
    } else {
      TestClass tc = (TestClass)sharedRoot;
      tc.tryLockBlock(1000);
      waitForAllToMoveOn();
    }
    
    waitForAllToMoveOn();
  }

  private void testCollectClientStatistics(String lockName, int index, int traceDepth) throws Throwable {
    if (index == 0) {
      waitForAllToMoveOn();
      connect();
      Thread.sleep(2000);
      verifyClientStat(ManagerUtil.getManager().generateLockIdentifier(lockName), 1, traceDepth);
      disconnect();
    } else {
      int count = 2;
      for (int i = 0; i < count; i++) {
        ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_READ);
      }
      for (int i = 0; i < count; i++) {
        ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_READ);
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
      // on windows, System.currentTimeMills() only changes every 15-16 millis! It’s even worse on windows 95 (~55ms)
      Assert.assertTrue(actualTime >= (expectedMinTime - 200));
    } else {
      Assert.assertTrue(actualTime >= (expectedMinTime - 10));
    }
  }
  
  private void testServerLockAggregateWaitTime(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      LockID lockID = ManagerUtil.getManager().generateLockIdentifier(lockName);
      long avgWaitTimeInMillis = getServerAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getServerAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      ThreadUtil.reallySleep(3000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testLockAggregateWaitTime(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      LockID lockID = ManagerUtil.getManager().generateLockIdentifier(lockName);
      long avgWaitTimeInMillis = getAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      ThreadUtil.reallySleep(3000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testBasicStatistics(String lockName, int index) throws Throwable {
    if (index == 0) {
      LockID lockID = ManagerUtil.getManager().generateLockIdentifier(lockName);
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
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForAllToMoveOn();
      waitForAllToMoveOn();

      waitForTwoToMoveOn();
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);

      waitForAllToMoveOn();

      waitForAllToMoveOn();

      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);

      waitForAllToMoveOn();

      waitForAllToMoveOn();

      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, Manager.LOCK_TYPE_WRITE);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorExit(lockName, Manager.LOCK_TYPE_WRITE);
    }

    waitForAllToMoveOn();
  }

  private void waitForAllToMoveOn() throws Exception {
    barrier.await();
  }

  private void waitForTwoToMoveOn() throws Exception {
    barrier2.await();
  }

  private void verifyLockRequest(LockID lock, int expectedValue) {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockRequested());
  }

  private void verifyLockAwarded(LockID lock, long expectedValue) {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockAwarded());
  }
  
  private long getServerAggregateAverageHeldTime(LockID lock) {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getServerStats().getAvgHeldTimeInMillis();
  }

  private long getAggregateAverageHeldTime(LockID lock) {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getClientStats().getAvgHeldTimeInMillis();
  }
  
  private long getServerAggregateAverageWaitTime(LockID lock) {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getServerStats().getAvgWaitTimeToAwardInMillis();
  }

  private long getAggregateAverageWaitTime(LockID lock) {
    LockSpec ls = getLockSpecFor(lock);
    return ls.getClientStats().getAvgWaitTimeToAwardInMillis();
  }

  private void verifyLockHop(LockID lock, int expectedValue) {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(expectedValue, ls.getClientStats().getNumOfLockHopRequests());
  }

  private void verifyClientStat(LockID lock, int numOfClientsStackTraces, int traceDepth) {
    LockSpec ls = getLockSpecFor(lock);
    Assert.assertEquals(numOfClientsStackTraces, ls.children().size());
    assertStackTracesDepth(ls.children(), traceDepth);
  }
  
  private void verifyClientStat(LockID lock, String lockType, int numOfClientsStackTraces, int traceDepth) {
    LockSpec ls = getLockSpecFor(lock);
//    Assert.assertEquals(lockType, ls.getObjectType());
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

  private LockSpec getLockSpecFor(LockID lock) {
    Collection<LockSpec> c = statMBean.getLockSpecs();
    System.err.println("Statistics For: ");
    for (LockSpec ls : c) {
      System.err.println("\t" + ls.getLockID());
      if (ls.getLockID().equals(lock)) {
        return ls;
      }
    }
    throw new AssertionError(lock + " cannot be found in the statistics.");    
  }
  
  private static class TestClass {
    private ReentrantLock rLock = new ReentrantLock();
    
    public synchronized void syncMethod(long time) {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }

    public void syncBlock(long time) {
      synchronized (this) {
        try {
          Thread.sleep(time);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
    
    public void nameLockMethod(long time) {
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
    
    public void tryLockBlock(long time) {
      boolean isLocked = rLock.tryLock();
      if (isLocked) {
        try {
          Thread.sleep(time);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        } finally {
          rLock.unlock();
        }
      }
    }
    
    public Object getTryLock() {
      return rLock;
    }
  }

}
