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

import java.util.Date;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueInterruptTakeTestApp extends AbstractTransparentApp {
  private static final int    DEFAULT_NUM_OF_LOOPS = 1000;

  private static final int    CAPACITY             = 100;

  private final int           numOfLoops;

  private final LinkedBlockingQueue   queue = new LinkedBlockingQueue(CAPACITY);
  private int                         count;
  private final CyclicBarrier         barrier;

  private int                 localCount;

  private transient Thread1 thread1;

  private volatile int node = -1;

  public LinkedBlockingQueueInterruptTakeTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    numOfLoops = DEFAULT_NUM_OF_LOOPS;
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      node = barrier.await();

      thread1 = new Thread1("Node "+node+" thread 1");
      thread1.start();
      Thread2 thread2 = new Thread2("Node "+node+" thread 2");
      thread2.start();

      System.out.println("Node "+node+" : waiting for thread 2");

      thread2.join();

      // wait for all thread 2 to be finished
      barrier.await();

      // wait for the queue to be empty
      synchronized (queue) {
        while (queue.size() > 0) {
          queue.wait(500);
        }
      }

      System.out.println("Node "+node+" : stopping thread 1");
      thread1.stopLoop();
      thread1.interrupt();

      System.out.println("Node "+node+" : waiting for thread 1");
      thread1.join();

      // wait for all thread 1 to be finished
      node = barrier.await();

      System.out.println(">>> Node "+node+" : Local count : "+localCount);

      if (0 == node) {
        System.out.println(">>> Total count : "+count);

        // check how many items have been taken from the queue
        int expected = numOfLoops * getParticipantCount();
        System.out.println("Took "+count+" from queue, expected "+expected);
        Assert.assertEquals(count, expected);
      }
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  public class Thread1 extends Thread {
    private volatile boolean stop = false;

    public Thread1(final String name) {
      super(name);
    }

    public void stopLoop() {
      this.stop = true;
    }

    public void run() {
      while (!stop) {
        Object taken = null;
        try {
//          System.out.println("Node "+node+" - thread 1 : "+queue.size()+" - taking");
          taken = queue.take();
          System.out.println("Node "+node+" - thread 1 : "+queue.size()+" - took : " + taken);
        } catch (InterruptedException e) {
          System.out.println("Node "+node+" - thread 1 : InterruptedException");
        } finally {
//          System.out.println("Node "+node+" - thread 1 : checking taken state");
          if (taken != null) {
            synchronized (queue) {
              queue.notifyAll();
              System.out.println("Node "+node+" - thread 1 : updating counts - "+(++localCount)+", "+(++count));
            }
          }
        }
      }
      System.out.println("Node "+node+" - thread 1 : finished");
    }
  }

  public class Thread2 extends Thread {
    public Thread2(final String name) {
      super(name);
    }

    public void run() {
      for (int i = 0; i < numOfLoops; i++) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          continue;
        }

        try {
          if (queue.size() >= 20) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              // do nothing
            }
          }
          Object o = new Date();
//          System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - putting : "+o);
          queue.put(o);
          System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - put : "+o);
          synchronized (queue) {
//            System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - checking queue size");
            if (0 == queue.size()) {
              System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - interrupting thread 1");
              thread1.interrupt();
//              System.out.println("Node "+node+" - thread 2 : "+queue.size()+" - interrupted thread 1");
            }
          }
        } catch (InterruptedException e) {
          System.out.println("Node "+node+" - thread 2 : InterruptedException");
        }
      }
      System.out.println("Node "+node+" - thread 2 : finished");
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
}