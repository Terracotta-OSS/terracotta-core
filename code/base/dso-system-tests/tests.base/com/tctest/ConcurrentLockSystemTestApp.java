/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ConcurrentLockSystemTestApp extends AbstractTransparentApp {

  private static final TCLogger logger       = TCLogging.getTestingLogger(ConcurrentLockSystemTestApp.class);
  private final TestObject      testObject   = new TestObject();
  private final SynchronizedInt participants = new SynchronizedInt(0);

  public ConcurrentLockSystemTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassname = ConcurrentLockSystemTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassname);
    spec.addRoot("testObject", testClassname + ".testObject");
    spec.addRoot("participants", testClassname + ".participants");

    String testObjectClassname = TestObject.class.getName();
    config.addIncludePattern(testObjectClassname);

    // create locks
    config.addWriteAutolock("* " + testClassname + ".run()");
    config.addWriteAutolock("* " + testObjectClassname + ".populate(..)");
    config.addReadAutolock("* " + testObjectClassname + ".isPopulated()");
    config.addAutolock("* " + testObjectClassname + ".increment()", ConfigLockLevel.CONCURRENT);

    // config for SynchronizedInt
    new SynchronizedIntSpec().visit(visitor, config);
  }

  public void run() {
    int participantCount = participants.increment();
    boolean isWriter = participantCount == 1;
    int iterations = 500;
    int children = 50;
    if (isWriter) {
      testObject.populate(children);
      for (int i = 0; i < iterations; i++) {
        if (i % (iterations / 5) == 0) info("incrementing TestObject in " + (i + 1) + " of " + iterations
                                            + " iterations.");
        testObject.increment();
      }
    }

    List counts;
    while ((counts = testObject.getAllCounts()).size() != children + 1) {
      System.err.println("Cycling until the counts list is large enough: " + counts);
      ThreadUtil.reallySleep(1 * 500);
    }

    List collapsedCounts;
    List expectedCounts = new LinkedList();
    expectedCounts.add(new Integer(iterations));
    while (true) {
      collapsedCounts = new LinkedList(new HashSet(testObject.getAllCounts()));
      if (expectedCounts.equals(collapsedCounts)) {
        System.err.println("SUCCESS.");
        return;
      }
      ThreadUtil.reallySleep(1 * 500);
    }
  }

  private void info(Object msg) {
    logger.info(this + ": " + msg);
  }

  private static final class TestObject {

    private TestObject child;
    private int        size;
    private int        count;

    public synchronized String toString() {
      return "TestObject[child=" + (child == null ? "null" : "TestObject") + ", size=" + size + ", count=" + count
             + "]";
    }

    public synchronized void populate(int populateCount) {
      if (isPopulated()) {
        info("TestObject already populated; not populating.");
        return;
      }
      info("Populating TestObject with " + populateCount + " children...");
      TestObject to = this;
      for (int i = 0; i < populateCount; i++) {
        synchronized (to) {
          to.child = new TestObject();
        }
        to = to.child;
      }
      this.size = populateCount;
      info("Done populating TestObject.");
    }

    public synchronized List getAllCounts() {
      TestObject to = this;
      List rv = new LinkedList();
      while (to != null) {
        rv.add(new Integer(count));
        to = to.child;
      }
      return rv;
    }

    public synchronized boolean isPopulated() {
      return this.child != null;
    }

    public void increment() {
      TestObject to = this;
      synchronized (this) {
        do {
          to.basicIncrement();
          to = to.child;
        } while (to != null);
      }
    }

    private void basicIncrement() {
      count++;
    }

    private void info(Object msg) {
      logger.info(this + ": " + msg);
    }

  }

}
