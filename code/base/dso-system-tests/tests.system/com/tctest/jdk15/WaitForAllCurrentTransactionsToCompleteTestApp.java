/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Exercise ManagerUtil.waitForAllCurrentTransactionsToComplete()
 * to see if screw up anything
 */
public class WaitForAllCurrentTransactionsToCompleteTestApp extends AbstractTransparentApp {
  private static final int          DEFAULT_NUM_OF_PUT  = 1234;
  private static final int          DEFAULT_NUM_OF_LOOP = 5;

  private static final int          CAPACITY            = 2000;

  private final LinkedBlockingQueue queue               = new LinkedBlockingQueue(CAPACITY);
  private final CyclicBarrier       barrier;

  private final int                 numOfPut;
  private final int                 numOfLoop;

  public WaitForAllCurrentTransactionsToCompleteTestApp(String appId, ApplicationConfig cfg,
                                                        ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());

    numOfPut = DEFAULT_NUM_OF_PUT;
    numOfLoop = DEFAULT_NUM_OF_LOOP;

    System.err.println("***** setting numOfPut=[" + numOfPut + "] numOfLoop=[" + numOfLoop + "]");
  }

  public void run() {
    try {
      int index = barrier.await();

      for (int i = 0; i < numOfLoop; i++) {
        if (index == 0) {
          doPut();
          waitTxnComplete();
          Assert.assertEquals(numOfPut + getParticipantCount() - 1, queue.size());
        }
        barrier.await();
        if (index != 0) {
          doGet();
          waitTxnComplete();
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
    System.out.println("XXX Client-" + ManagerUtil.getClientID() + " waitForAllCurrentTransactionsToComplete took "
                       + (System.currentTimeMillis() - now) + "ms");
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
