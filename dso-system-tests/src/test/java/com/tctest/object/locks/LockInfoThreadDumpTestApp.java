/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.object.locks;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class LockInfoThreadDumpTestApp extends AbstractTransparentApp {
  protected static final String JMXPORT = "jmxport";
  private final int             jmxPort;
  private final Object          lock1, lock2, lock3;
  private final CyclicBarrier   barrier;
  private final CountDown       count;
  private int                   myIndex = -1;
  private L1InfoMBean           l1Info;

  public LockInfoThreadDumpTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    Assert.eval(3 == getParticipantCount());
    barrier = new CyclicBarrier(getParticipantCount());
    jmxPort = Integer.valueOf(cfg.getAttribute(JMXPORT));
    count = new CountDown(getParticipantCount());
    lock1 = new Object();
    lock2 = new Object();
    lock3 = new Object();
  }

  protected void setUp() throws Exception {
    myIndex = barrier.await();
    System.out.println("XXX my id = " + myIndex);
  }

  public void run() {
    try {
      setUp();
      testDeadLock();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    // roots
    config.addRoot("lock1", LockInfoThreadDumpTestApp.class.getName() + ".lock1");
    config.addRoot("lock2", LockInfoThreadDumpTestApp.class.getName() + ".lock2");
    config.addRoot("lock3", LockInfoThreadDumpTestApp.class.getName() + ".lock3");
    config.addRoot("count", LockInfoThreadDumpTestApp.class.getName() + ".count");
    config.addRoot("barrier", LockInfoThreadDumpTestApp.class.getName() + ".barrier");

    // methods
    config.addWriteAutolock("* " + LockInfoThreadDumpTestApp.class.getName() + ".*(..)");
    config.addWriteAutolock("* " + LockInfoThreadDumpTestApp.LockInfoDeadLockAction.class.getName() + ".*(..)");
    config.addWriteAutolock("* " + CountDown.class.getName() + ".*(..)");

    // includes
    config.addIncludePattern(LockInfoThreadDumpTestApp.class.getName());
    config.addIncludePattern(LockInfoThreadDumpTestApp.LockInfoDeadLockAction.class.getName());
    config.addIncludePattern(Object.class.getName());
    config.addIncludePattern(CountDown.class.getName());
    config.addIncludePattern(CyclicBarrier.class.getName());
  }

  public void testDeadLock() {

    if (myIndex == 0) {
      l1Info = getL1InfoBean(0);
      new LockInfoDeadLockAction(lock1, lock2, "DEAD_LOCK_THREAD_0").start();
    } else if (myIndex == 1) {
      l1Info = getL1InfoBean(1);
      new LockInfoDeadLockAction(lock2, lock3, "DEAD_LOCK_THREAD_1").start();
    } else if (myIndex == 2) {
      l1Info = getL1InfoBean(2);
      new LockInfoDeadLockAction(lock3, lock1, "DEAD_LOCK_THREAD_2").start();
    }

    ThreadUtil.reallySleep(2000);
    waitForMBeanReady(l1Info, 60);
    String threadDump = l1Info.takeThreadDump(System.currentTimeMillis());
    ThreadUtil.reallySleep(2000);

    Assert.eval("The text \"LOCKED : [\" should be present in the thread dump", threadDump.indexOf("LOCKED : [") >= 0);

    Assert.eval("The text \"WAITING TO LOCK: [\" should be present in the thread dump",
                threadDump.indexOf("WAITING TO LOCK: [") >= 0);

    System.out.println("XXX DONE");
  }

  // wait till mbean ready
  private void waitForMBeanReady(L1InfoMBean bean, int maxWaitSec) {
    while (maxWaitSec-- >= 0) {
      try {
        bean.getVersion();
        break;
      } catch (UndeclaredThrowableException e) {
        if (maxWaitSec == 0) throw e;
        ThreadUtil.reallySleep(1000);
      }
    }
  }

  private L1InfoMBean getL1InfoBean(int clientID) {
    MBeanServerConnection mbsc;
    try {
      JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
      mbsc = jmxc.getMBeanServerConnection();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    DSOMBean dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO,
                                                                                 DSOMBean.class, false);

    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = (DSOClientMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i],
                                                                                  DSOClientMBean.class, false);
    }

    return (L1InfoMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, clients[clientID].getL1InfoBeanName(),
                                                                       L1InfoMBean.class, false);
  }

  class LockInfoDeadLockAction extends Thread {

    private final Object obj1, obj2;

    public LockInfoDeadLockAction(Object lock1, Object lock2, String name) {
      this.obj1 = lock1;
      this.obj2 = lock2;
      this.setName(name);
    }

    @Override
    public void run() {

      try {
        synchronized (obj1) {
          count.release();
          count.acquire();
          synchronized (obj2) {
            throw new Error("Not supposed to get here");
          }
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }

    }
  }

}
