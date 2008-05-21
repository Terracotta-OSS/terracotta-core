/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueInterruptTakeTestApp extends AbstractTransparentApp {
  private static final int    DEFAULT_NUM_OF_LOOPS = 2000;

  private static final int    CAPACITY             = 100;

  private final int           numOfLoops;

  private LinkedBlockingQueue queue                = new LinkedBlockingQueue(CAPACITY);
  private int                 count;
  private final CyclicBarrier barrier;
  private final Map           nodes;

  private final transient Thread1 thread1          = new Thread1();

  private volatile int node = -1;

  public LinkedBlockingQueueInterruptTakeTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    numOfLoops = DEFAULT_NUM_OF_LOOPS;
    barrier = new CyclicBarrier(getParticipantCount());
    nodes = new ConcurrentHashMap();
  }

  public void run() {
    try {
      node = barrier.await();

      Integer node_integer = new Integer(node);
      nodes.put(node_integer, this);

      thread1.start();
      Thread2 thread2 = new Thread2();
      thread2.start();

      System.out.println("Node "+node+" : waiting for thread 2");

      thread2.join();

      System.out.println("Node "+node+" : stopping thread 1");
      thread1.stopLoop();

      System.out.println("Node "+node+" : waiting for thread 1");
      if (1 == nodes.size() &&
          0 == queue.size()) {
        thread1.interrupt();
      }
      thread1.join();

      nodes.remove(node_integer);
      node = barrier.await();

      if (0 == node) {
        synchronized (queue) {
          int expected = numOfLoops * getParticipantCount();
          System.out.println("Took "+count+" from queue, expected "+expected);
          Assert.assertEquals(count, expected);
        }
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public class Thread1 extends Thread {
    private boolean stop = false;

    public void stopLoop() {
      this.stop = true;
    }

    public void run() {
      while (!stop) {
        try {
          System.out.println("Node "+node+" : "+queue.size()+" - took : " + queue.take());
          synchronized (queue) {
            count++;
          }
        } catch (InterruptedException e) {
          System.out.println("Node "+node+" : thread 1 InterruptedException");
        }
      }
      System.out.println("Node "+node+" : thread 1 finished");
    }
  }

  public class Thread2 extends Thread {
    public void run() {
      for (int i = 0; i < numOfLoops; i++) {
        try {
          Thread.sleep(10);

          if (queue.size() >= 20) {
            Thread.sleep(500);
          }
          Object o = new Date();
          queue.put(o);
          System.out.println("Node "+node+" : "+queue.size()+" - put : "+o);
          if (0 == queue.size()) {
            synchronized (queue) {
              thread1.interrupt();
            }
          }
        } catch (InterruptedException e) {
          System.out.println("Node "+node+" : thread 2 InterruptedException");
        }
      }
      System.out.println("Node "+node+" : thread 2 finished");
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LinkedBlockingQueueInterruptTakeTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("queue", "queue");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("count", "count");
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