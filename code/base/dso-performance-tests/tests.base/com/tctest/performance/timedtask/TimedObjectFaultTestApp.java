/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.timedtask;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ITransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public final class TimedObjectFaultTestApp extends AbstractTransparentApp {

  private final BlockingQueue rootQueue;
  private final CyclicBarrier barrier;
  private final CyclicBarrier writerBarrier;
  private final int           writers;
  private int                 writerCounter;
  private boolean             isLocalWriter, isMasterNode;
  private long                starttime, endtime;
  private static final int    VOLUME  = 10000;
  private static final double NANOSEC = 1000000000D;

  public TimedObjectFaultTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    writers = getIntensity();
    barrier = new CyclicBarrier(getParticipantCount());
    writerBarrier = new CyclicBarrier(writers);
    rootQueue = new LinkedBlockingQueue(100);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    ITransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    String className = TimedObjectFaultTestApp.class.getName();
    spec = config.getOrCreateSpec(className);

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    spec.addRoot("rootQueue", "rootQueue");
    spec.addRoot("writerCounter", "writerCounter");
    spec.addRoot("starttime", "starttime");
    spec.addRoot("endtime", "endtime");
    spec.addRoot("barrier", "barrier");
    spec.addRoot("writerBarrier", "writerBarrier");
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(CyclicBarrier.class.getName());
    String className = TimedObjectFaultTestApp.class.getName();
    config.addIncludePattern(className);

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    config.addRoot("rootQueue", className + ".rootQueue");
    config.addRoot("writerCounter", className + ".writerCounter");
    config.addRoot("starttime", className + ".starttime");
    config.addRoot("endtime", className + ".endtime");
    config.addRoot("barrier", className + ".barrier");
    config.addRoot("writerBarrier", className + ".writerBarrier");
  }

  public void run() {
    synchronized (barrier) {
      if (++writerCounter <= writers) {
        if (writerCounter == 1) isMasterNode = true;
        isLocalWriter = true;
      }
    }

    try {

      barrier.await();

      if (isLocalWriter) writer();
      else reader();

      barrier.await();

    } catch (Throwable t) {
      notifyError(t);
    }

    if (isMasterNode) {
      printResult(starttime, endtime);
    }
  }

  private void writer() throws InterruptedException, BrokenBarrierException {
    int iterations = VOLUME / writers;
    starttime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      rootQueue.put(new Object());
    }

    writerBarrier.await();

    if (isMasterNode) {
      for (int i = 0; i < getParticipantCount() - writers; i++) {
        rootQueue.put("stop");
      }
    }
  }

  private void reader() throws InterruptedException {
    while (true) {
      if (rootQueue.take() instanceof String) break;
    }
    endtime = System.nanoTime();
  }

  private void printResult(long start, long end) {
    double time = (end - start);
    long result = Math.round(VOLUME / (time / NANOSEC));
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: value=" + result + " obj/sec");
  }
}
