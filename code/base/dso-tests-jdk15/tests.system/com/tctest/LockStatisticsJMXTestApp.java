/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;
import com.tc.management.lock.stats.LockStatElement;
import com.tc.object.LiteralValues;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class LockStatisticsJMXTestApp extends AbstractTransparentApp {
  private static final LiteralValues LITERAL_VALUES   = new LiteralValues();
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

      String lockName = "lock0";
      testServerLockAggregateWaitTime(lockName, index);

      enableStackTraces(index, 1, 1);

      lockName = "lock1";
      testLockAggregateWaitTime(lockName, index);

      lockName = "lock2";
      testBasicStatistics(lockName, index);

      lockName = "lock3";

      enableStackTraces(index, 2, 1);

      testCollectClientStatistics(lockName, index, 1);

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
      verifyClientStat(NAMED_LOCK_NAME, 1, traceDepth);
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
      String lockID = ByteCodeUtil.generateAutolockName(((Manageable)sharedRoot).__tc_managed().getObjectID());
      verifyClientStat(lockID, TestClass.class.getName(), 2, traceDepth);
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
      String lockID = ByteCodeUtil.generateAutolockName(((Manageable)tc.getTryLock()).__tc_managed().getObjectID());
      verifyClientStat(lockID, ReentrantLock.class.getName(), 1, traceDepth);
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
      verifyClientStat(ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName), 1, traceDepth);
      disconnect();
    } else {
      int count = 2;
      for (int i = 0; i < count; i++) {
        ManagerUtil.monitorEnter(lockName, LockLevel.READ);
      }
      for (int i = 0; i < count; i++) {
        ManagerUtil.monitorExit(lockName);
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
      String lockID = ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName);
      long avgWaitTimeInMillis = getServerAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getServerAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      ThreadUtil.reallySleep(3000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testLockAggregateWaitTime(String lockName, int index) throws Throwable {
    if (index == 0) {
      connect();
      waitForAllToMoveOn();
      String lockID = ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName);
      long avgWaitTimeInMillis = getAggregateAverageWaitTime(lockID);
      long avgHeldTimeInMillis = getAggregateAverageHeldTime(lockID);

      System.out.println("avgHeldTimeInMillis: " + avgHeldTimeInMillis);
      System.out.println("avgWaitTimeInMillis: " + avgWaitTimeInMillis);
      assertExpectedTime(1000, avgWaitTimeInMillis);
      assertExpectedTime(2000, avgHeldTimeInMillis);
    } else if (index == 1) {
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      ThreadUtil.reallySleep(3000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      ThreadUtil.reallySleep(2000);
      ManagerUtil.monitorExit(lockName);
      waitForAllToMoveOn();
    }
    waitForAllToMoveOn();
  }

  private void testBasicStatistics(String lockName, int index) throws Throwable {
    if (index == 0) {
      String lockID = ByteCodeUtil.generateLiteralLockName(LITERAL_VALUES.valueFor(lockName), lockName);
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
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForAllToMoveOn();
      waitForAllToMoveOn();

      waitForTwoToMoveOn();
      ManagerUtil.monitorExit(lockName);

      waitForAllToMoveOn();

      waitForAllToMoveOn();

      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
    } else if (index == 2) {
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);

      waitForAllToMoveOn();

      waitForAllToMoveOn();

      Thread.sleep(1000);
      ManagerUtil.monitorExit(lockName);
      waitForTwoToMoveOn();
      ManagerUtil.monitorEnter(lockName, LockLevel.WRITE);
      waitForTwoToMoveOn();
      waitForAllToMoveOn();
      waitForAllToMoveOn();
      ManagerUtil.monitorExit(lockName);
    }

    waitForAllToMoveOn();
  }

  private void waitForAllToMoveOn() throws Exception {
    barrier.await();
  }

  private void waitForTwoToMoveOn() throws Exception {
    barrier2.await();
  }

  private void verifyLockRequest(String lockName, int expectedValue) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        Assert.assertEquals(expectedValue, lsi.getClientStats().getNumOfLockRequested());
        return;
      }
    }
    throw new AssertionError(lockName + " cannot be found in the statistics.");
  }

  private void verifyLockAwarded(String lockName, long expectedValue) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        Assert.assertEquals(expectedValue, lsi.getClientStats().getNumOfLockAwarded());
        return;
      }
    }
    throw new AssertionError(lockName + " cannot be found in the statistics.");
  }
  
  private long getServerAggregateAverageHeldTime(String lockName) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        return lsi.getServerStats().getAvgHeldTimeInMillis();
      }
    }
    return -1;
  }

  private long getAggregateAverageHeldTime(String lockName) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        return lsi.getClientStats().getAvgHeldTimeInMillis();
      }
    }
    return -1;
  }
  
  private long getServerAggregateAverageWaitTime(String lockName) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        return lsi.getServerStats().getAvgWaitTimeToAwardInMillis();
      }
    }
    return -1;
  }

  private long getAggregateAverageWaitTime(String lockName) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        return lsi.getClientStats().getAvgWaitTimeToAwardInMillis();
      }
    }
    return -1;
  }

  private void verifyLockHop(String lockName, int expectedValue) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().equals(lockName)) {
        Assert.assertEquals(expectedValue, lsi.getClientStats().getNumOfLockHopRequests());
        return;
      }
    }
    throw new AssertionError(lockName + " cannot be found in the statistics.");
  }

  private void verifyClientStat(String lockName, int numOfClientsStackTraces, int traceDepth) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().indexOf(lockName) != -1) {
        Assert.assertEquals(numOfClientsStackTraces, lsi.children().size());
        assertStackTracesDepth(lsi.children(), traceDepth);
        return;
      }
    }
  }
  
  private void verifyClientStat(String lockName, String lockType, int numOfClientsStackTraces, int traceDepth) {
    Collection c = statMBean.getLockSpecs();
    for (Iterator i = c.iterator(); i.hasNext();) {
      LockSpec lsi = (LockSpec) i.next();
      if (lsi.getLockID().asString().indexOf(lockName) != -1) {
        Assert.assertEquals(lockType, lsi.getObjectType());

        Assert.assertEquals(numOfClientsStackTraces, lsi.children().size());
        assertStackTracesDepth(lsi.children(), traceDepth);
        return;
      }
    }
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
