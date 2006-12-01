/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.timedtask;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class TimedSingleQueueThroughputTestApp extends AbstractTransparentApp {

  private static final int    VOLUME    = 10000;
  private static final double NANOSEC   = 1000000000D;
  private final BlockingQueue rootQueue;

  public TimedSingleQueueThroughputTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    rootQueue = new LinkedBlockingQueue(100);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String className = TimedSingleQueueThroughputTestApp.class.getName();
    config.addRoot("rootQueue", className + ".rootQueue");
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String className = TimedSingleQueueThroughputTestApp.class.getName();
    config.addRoot("rootQueue", className + ".rootQueue");
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

  public void run() {
    Thread producer = new Thread() {
      public void run() {
        try {
          for (int i = 0; i < 100; i++) {
            populate(new Object());
          }
          long start = System.nanoTime();
          for (int i = 0; i < VOLUME; i++) {
            populate(new Object());
          }
          long end = System.nanoTime();
          for (int i = 0; i < 100; i++) {
            populate(new Object());
          }

          printResult(start, end);

        } catch (Throwable t) {
          notifyError(t);
        }
      }
    };
    producer.setDaemon(true);

    Thread consumer = new Thread() {
      public void run() {
        try {
          while (true) {
            retrieve();
          }
        } catch (Throwable t) {
          notifyError(t);
        }
      }
    };
    consumer.setDaemon(true);

    producer.start();
    consumer.start();

    try {
      producer.join();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void populate(Object data) throws InterruptedException {
    rootQueue.put(data);
  }

  private void retrieve() throws InterruptedException {
    rootQueue.take();
  }

  private void printResult(long start, long end) {
    double time = (end - start);
    long result = Math.round(VOLUME / (time / NANOSEC));
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: value=" + result + " obj/sec");
  }
}
