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

import java.util.concurrent.CyclicBarrier;

public final class TimedChangeReplicationTestApp extends AbstractTransparentApp {

  private static final int    SIZE    = 100;
  private static final int    VOLUME  = 1000;
  private static final double NANOSEC = 1000000000D;
  private boolean             isLocalWriter;
  private boolean             isSharedReader;
  private final DataRoot      root;
  private final CyclicBarrier barrier;

  public TimedChangeReplicationTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
    root = new DataRoot();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    ITransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    String className = TimedChangeReplicationTestApp.class.getName();
    spec = config.getOrCreateSpec(className);

    config.addIncludePattern(className + "$*");

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    config.addWriteAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "increment*(..)");
    config.addWriteAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "waitForWriter*(..)");
    config.addReadAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "touch*(..)");

    spec.addRoot("isSharedReader", "isSharedReader");
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
  }

  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    config.addIncludePattern(CyclicBarrier.class.getName());
    String className = TimedChangeReplicationTestApp.class.getName();
    config.addIncludePattern(className);

    config.addIncludePattern(className + "$*");

    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");
    config.addWriteAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "increment*(..)");
    config.addWriteAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "waitForWriter*(..)");
    config.addReadAutolock("* " + TimedChangeReplicationTestApp.DataRoot.class.getName() + "touch*(..)");

    config.addRoot("isSharedReader", className + ".isSharedReader");
    config.addRoot("root", className + ".root");
    config.addRoot("barrier", className + ".barrier");
  }

  public void run() {
    System.out.println("root hydrated: " + root); // hydrate

    synchronized (root) {
      if (!isSharedReader) {
        isSharedReader = true; // make others writers
        isLocalWriter = true;
      }
    }

    try {
      replicateChanges();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void replicateChanges() throws Throwable {
    barrier.await();

    long start = System.nanoTime();
    for (int i = 0; i < VOLUME; i++) {
      if (isLocalWriter) root.increment();
      else root.touch();

      barrier.await();

      if (isLocalWriter) root.waitForWriter();
    }

    if (isLocalWriter) {
      long end = System.nanoTime();

      printResult(start, end);
    }
  }

  private class DataRoot {

    private boolean   waitForWriter = true;
    private Element[] elements      = new Element[SIZE];

    private DataRoot() {
      for (int i = 0; i < SIZE; i++) {
        elements[i] = new Element();
      }
    }

    private synchronized void increment() {
      for (int i = 0; i < SIZE; i++) {
        elements[i].counter++;
      }
      waitForWriter = false;
      notifyAll();
    }

    private synchronized void touch() throws Exception {
      while (waitForWriter)
        wait();
      // obtaining lock will replicate change to this node
    }

    private synchronized void waitForWriter() {
      waitForWriter = true;
    }
  }

  private class Element {
    private int counter;
  }

  private void printResult(long start, long end) {
    double time = (end - start);
    long result = Math.round(VOLUME / (time / NANOSEC));
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: value=" + result + " replications/sec with " + SIZE
                       + " field deltas");
  }
}
