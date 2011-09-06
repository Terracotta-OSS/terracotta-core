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
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueSingleNodeTestApp extends AbstractTransparentApp {
  private static final int    NUM_OF_PUTS   = 1000;
  private static final int    NUM_OF_LOOPS  = 1;
  private static final int    NUM_OF_PUTTER = 1;
  private static final int    NUM_OF_GETTER = 1;

  private LinkedBlockingQueue queue         = new LinkedBlockingQueue();

  public LinkedBlockingQueueSingleNodeTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      CyclicBarrier barrier = new CyclicBarrier(NUM_OF_PUTTER+NUM_OF_GETTER+1);
      for (int i = 0; i < NUM_OF_LOOPS; i++) {
        Thread[] putters = new Thread[NUM_OF_PUTTER];
        Thread[] getters = new Thread[NUM_OF_GETTER];
        for (int j = 0; j < NUM_OF_PUTTER; j++) {
          putters[j] = new Thread(new Putter(barrier, queue, NUM_OF_GETTER));
        }
        for (int j = 0; j < NUM_OF_GETTER; j++) {
          getters[j] = new Thread(new Getter(barrier, queue));
        }
        for (int j = 0; j < NUM_OF_PUTTER; j++) {
          putters[j].start();
        }
        for (int j = 0; j < NUM_OF_GETTER; j++) {
          getters[j].start();
        }
      }
      barrier.await();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private static class Getter implements Runnable {
    private LinkedBlockingQueue queue;
    private CyclicBarrier       barrier;

    public Getter(CyclicBarrier barrier, LinkedBlockingQueue queue) {
      this.barrier = barrier;
      this.queue = queue;
    }

    public void run() {
      try {
        while (true) {
          Object o = queue.take();
          if ("STOP".equals(o)) {
            break;
          }
          WorkItem w = (WorkItem) o;
          System.out.println("Getting " + w.getI());
        }
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  private static class Putter implements Runnable {
    private CyclicBarrier       barrier;
    private LinkedBlockingQueue queue;
    private int                 numOfGetter;

    public Putter(CyclicBarrier barrier, LinkedBlockingQueue queue, int numOfGetter) {
      this.barrier = barrier;
      this.queue = queue;
      this.numOfGetter = numOfGetter;
    }

    public void run() {
      try {
        for (int i = 0; i < NUM_OF_PUTS; i++) {
          System.out.println("Putting " + i);
          queue.put(new WorkItem(i));
        }
        for (int i = 0; i < numOfGetter; i++) {
          queue.put("STOP");
        }
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueSingleNodeTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
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
