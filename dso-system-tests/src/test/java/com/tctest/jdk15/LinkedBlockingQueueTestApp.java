/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueTestApp extends AbstractTransparentApp {
  public static final String        GC_TEST_KEY         = "gc-test";

  private static final int          DEFAULT_NUM_OF_PUT  = 1000;
  private static final int          DEFAULT_NUM_OF_LOOP = 5;

  private static final int          GC_NUM_OF_PUT       = 1000;
  private static final int          GC_NUM_OF_LOOP      = 5;
  private static final int          GC_CREATE_NUM       = 5;

  private static final int          CAPACITY            = 100;

  private final LinkedBlockingQueue queue               = new LinkedBlockingQueue(CAPACITY);
  private final CyclicBarrier       barrier;

  private boolean                   isGcTest            = false;
  private final int                 gcCreateNum;
  private final int                 numOfPut;
  private final int                 numOfLoop;

  public LinkedBlockingQueueTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());

    Boolean booleanObject = Boolean.valueOf(cfg.getAttribute(GC_TEST_KEY));
    isGcTest = booleanObject.booleanValue();

    if (isGcTest) {
      gcCreateNum = GC_CREATE_NUM;
      numOfPut = GC_NUM_OF_PUT;
      numOfLoop = GC_NUM_OF_LOOP;
    } else {
      gcCreateNum = 1;
      numOfPut = DEFAULT_NUM_OF_PUT;
      numOfLoop = DEFAULT_NUM_OF_LOOP;
    }

    System.err.println("***** setting isGcTest=[" + isGcTest + "]  gcCreateNum=[" + gcCreateNum + "] numOfPut=["
                       + numOfPut + "] numOfLoop=[" + numOfLoop + "]");
  }

  public void run() {
    try {
      int index = barrier.await();

      for (int j = 0; j < gcCreateNum; j++) {
        for (int i = 0; i < numOfLoop; i++) {
          if (index == 0) {
            doPut();
          } else {
            doGet();
          }
          barrier.await();
        }
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

    @SuppressWarnings("unused")
    public int getI() {
      return this.i;
    }
  }
}
