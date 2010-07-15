/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

/**
 * Exercise ManagerUtil.waitForAllCurrentTransactionsToComplete() to see if screw up anything
 */
public class WaitForAllCurrentTransactionsToCompleteTestApp extends AbstractTransparentApp {
  private static final TCLogger     logger              = TCLogging.getLogger("com.tc.WaitForAllCurrentTransactionsToComplete");
  private static final int          DEFAULT_NUM_OF_PUT  = 1234;
  private static final int          DEFAULT_NUM_OF_LOOP = 5;
  public static final String        JMX_PORT            = "jmx-port";

  private static final int          CAPACITY            = 2000;

  private final LinkedBlockingQueue queue               = new LinkedBlockingQueue(CAPACITY);
  private final ApplicationConfig   appConfig;
  private final CyclicBarrier       barrier;

  private final int                 numOfPut;
  private final int                 numOfLoop;

  public WaitForAllCurrentTransactionsToCompleteTestApp(String appId, ApplicationConfig cfg,
                                                        ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.appConfig = cfg;
    barrier = new CyclicBarrier(getParticipantCount());

    numOfPut = DEFAULT_NUM_OF_PUT;
    numOfLoop = DEFAULT_NUM_OF_LOOP;

    logger.info("***** setting numOfPut=[" + numOfPut + "] numOfLoop=[" + numOfLoop + "]");
  }

  public void run() {
    try {
      int index = barrier.await();

      for (int i = 0; i < numOfLoop; i++) {
        if (index == 0) {
          doPut();
          Assert.assertEquals(numOfPut + getParticipantCount() - 1, queue.size());
          logger.info("XXX Txns in the system");
          getPendingTransactionsCount();
        }
        barrier.await();
        waitTxnComplete();
        barrier.await();
        if (index == 0) {
          ThreadUtil.reallySleep(1000);
          logger.info("XXX Txns in the system, after txn complete");
          Assert.assertEquals(0, getPendingTransactionsCount());
        }
        barrier.await();
        if (index != 0) {
          doGet();
          Assert.assertTrue(queue.size() < (getParticipantCount() - 1));
        }
        barrier.await();
      }

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void waitTxnComplete() {
    long now = System.currentTimeMillis();
    ManagerUtil.waitForAllCurrentTransactionsToComplete();
    logger.info("XXX Client-" + ManagerUtil.getClientID() + " waitForAllCurrentTransactionsToComplete took "
                + (System.currentTimeMillis() - now) + "ms");
  }

  private long getPendingTransactionsCount() {
    MBeanServerConnection mbsc;
    JMXConnectorProxy jmxc = new JMXConnectorProxy("localhost", Integer.valueOf(appConfig.getAttribute(JMX_PORT)));
    try {
      mbsc = jmxc.getMBeanServerConnection();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    DSOMBean dsoMBean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO,
                                                                                 DSOMBean.class, false);
    Map<ObjectName, Long> map = dsoMBean.getAllPendingTransactionsCount();

    long l = -1;
    for (ObjectName o : map.keySet()) {
      logger.info("Pending Tranaction count for " + o + " size: " + map.get(o));
      String cid = o.getKeyProperty("channelID");
      if (cid.equals(ManagerUtil.getClientID())) {
        l = map.get(o);
        logger.info("XXX Pending Tranaction count for channleID=" + cid + " size: " + l);
      }
    }
    return l;
  }

  private void doGet() throws Exception {
    while (true) {
      Object o = queue.take();
      if ("STOP".equals(o)) {
        break;
      }
    }
  }

  private void doPut() throws Exception {
    for (int i = 0; i < numOfPut; i++) {
      // System.out.println("Putting " + i);
      queue.put(new WorkItem(i));
      Assert.assertTrue(queue.size() <= CAPACITY);
    }
    int numOfGet = getParticipantCount() - 1;
    for (int i = 0; i < numOfGet; i++) {
      queue.put("STOP");
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = WaitForAllCurrentTransactionsToCompleteTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
    spec.addRoot("barrier", "barrier");
  }

  private static class WorkItem {
    private final int i;

    public WorkItem(int i) {
      this.i = i;
    }

    @SuppressWarnings("unused")
    public int getI() {
      return this.i;
    }
  }
}
