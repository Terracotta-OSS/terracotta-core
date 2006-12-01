/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueTestApp extends AbstractTransparentApp {
  private static final int    NUM_OF_PUTS  = 1000;
  private static final int    NUM_OF_LOOPS = 5;

  private LinkedBlockingQueue queue        = new LinkedBlockingQueue(100);
  private final CyclicBarrier barrier;

  public LinkedBlockingQueueTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.await();

      for (int i = 0; i < NUM_OF_LOOPS; i++) {
        if (index == 0) {
          doPut();
        } else {
          doGet();
        }
        barrier.await();
      }

      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void doGet() throws Exception {
    while (true) {
      Object o = queue.take();
      if ("STOP".equals(o)) {
        break;
      }
      WorkItem w = (WorkItem) o;
      System.out.println("Getting " + w.getI());
    }
  }

  private void doPut() throws Exception {
    for (int i = 0; i < NUM_OF_PUTS; i++) {
      System.out.println("Putting " + i);
      queue.put(new WorkItem(i));
    }
    int numOfGet = getParticipantCount() - 1;
    for (int i = 0; i < numOfGet; i++) {
      queue.put("STOP");
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueTestApp.class.getName();
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

    public int getI() {
      return this.i;
    }
  }
}
