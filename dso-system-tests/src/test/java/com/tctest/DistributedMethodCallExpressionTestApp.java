/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.config.spec.SynchronizedIntSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

/**
 * This test is to test the various method expressions for DistributedMethodCall. The test will also make sure that
 * constructors and static methods will not be called.
 */
public class DistributedMethodCallExpressionTestApp extends AbstractTransparentApp {

  private final SharedModel   model             = new SharedModel();
  private final int           initialNodeCount  = getParticipantCount();
  private final CyclicBarrier nodeBarrier       = new CyclicBarrier(initialNodeCount);
  private final CyclicBarrier staticNodeBarrier = new CyclicBarrier(initialNodeCount);

  public DistributedMethodCallExpressionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    try {
      callNonStaticMethod();
      callStaticMethod();
    } catch (Throwable e) {
      notifyError(e);
    }
  }

  public void callNonStaticMethod() throws Throwable {
    final boolean masterNode = nodeBarrier.barrier() == 0;
    synchronized (model) {
      if (masterNode) {
        model.nonStaticMethod(null, 0, 0, null, null, false);
      }
    }
    nodeBarrier.barrier();
    System.err.println("\n### initialNodeCount = " + initialNodeCount);
    synchronized (model) {
      checkCountTimed(model.nonStaticCallCount, initialNodeCount, 10, 5 * 1000, "Non-Static Call Count");
    }
    nodeBarrier.barrier();
  }

  public void callStaticMethod() throws Throwable {
    final boolean masterNode = staticNodeBarrier.barrier() == 0;
    synchronized (model) {
      if (masterNode) {
        SharedModel.staticMethod();
      }
    }
    staticNodeBarrier.barrier();
    System.err.println("### -> sleeping before checking static method calls");
    Thread.sleep(30000);
    synchronized (model) {
      checkCountTimed(SharedModel.staticCallCount, (masterNode) ? 1 : 0, 1, 10000, "Static Call Count");
    }
    staticNodeBarrier.barrier();
  }

  public static class SharedModel {
    public final SynchronizedInt        nonStaticCallCount = new SynchronizedInt(0);
    public static final SynchronizedInt staticCallCount    = new SynchronizedInt(0);

    public static void staticMethod() {
      staticCallCount.increment();
    }

    public void nonStaticMethod(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b)
        throws Throwable {
      nonStaticCallCount.increment();
    }
  }

  private void checkCountTimed(SynchronizedInt actualSI, final int expected, final int slices, final long sliceMillis,
                               String msg) throws InterruptedException {
    // wait until all nodes have the right picture of the cluster
    int actual = 0;
    int i;
    for (i = 0; i < slices; i++) {
      actual = actualSI.get();
      if (actual > expected || actual < 0) {
        notifyError("Wrong Count: expected=" + expected + ", actual=" + actual);
      }
      if (actual < expected) {
        Thread.sleep(sliceMillis);
      } else {
        break;
      }
    }
    if (i == slices) {
      notifyError("Wrong Count: expected=" + expected + ", actual=" + actual);
    }
    System.err.println("\n### -> check '" + msg + "' passed in " + i + " slices");
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      new CyclicBarrierSpec().visit(visitor, config);
      new SynchronizedIntSpec().visit(visitor, config);

      TransparencyClassSpec spec = config.getOrCreateSpec(FooObject.class.getName());
      String testClassName = DistributedMethodCallExpressionTestApp.class.getName();
      spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("model", "model");
      spec.addRoot("nodeBarrier", "nodeBarrier");
      spec.addRoot("staticNodeBarrier", "staticNodeBarrier");
      String methodExpression = "* " + testClassName + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedModel.class.getName());
      spec.addDistributedMethodCall("staticMethod", "()V", true);
      try {
        spec.addDistributedMethodCall("<init>", "()V", true);
        throw new AssertionError("Should have thrown an AssertionError.");
      } catch (AssertionError e) {
        // Expected.
      }
      
      config
          .addDistributedMethodCall(new DistributedMethodSpec("* com.tctest.DistributedMethodCallExpressionTestApp$SharedModel.nonStaticMethod(..)", true));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
