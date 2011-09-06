/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

public class NullLiteralArrayElementRegressionTestApp extends AbstractTransparentApp {

  private final TestObject    root = new TestObject();
  private final CyclicBarrier barrier;

  public NullLiteralArrayElementRegressionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);

    if (getParticipantCount() != 3) {
      // must have 3 nodes for this test to work
      throw new RuntimeException("wrong number of nodes: " + getParticipantCount());
    }

    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      test();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void test() throws Exception {
    // Get the root object paged into each node (and strongly held) before creating the array
    TestObject obj = root;

    final boolean creator;
    synchronized (obj) {
      if (!obj.hasArray()) {
        creator = true;
        obj.setArray(new Object[10]);
      } else {
        creator = false;
      }
    }

    barrier.barrier();

    if (creator) {
      synchronized (obj) {
        obj.setElement(5, 42L);
      }
    }

    barrier.barrier();

    Object value = obj.getElement(5);

    if (value == null) { throw new NullPointerException("element is null"); }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = NullLiteralArrayElementRegressionTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
    config.addIncludePattern(TestObject.class.getName());

    new CyclicBarrierSpec().visit(visitor, config);
  }

  private static class TestObject {
    private Object[] array;

    boolean hasArray() {
      return this.array != null;
    }

    void setArray(Object[] a) {
      this.array = a;
    }

    Object getElement(int index) {
      return this.array[index];
    }

    // This method takes a long b/c that is a "literal" type, but stored in an Object array
    void setElement(int index, long value) {
      array[index] = Long.valueOf(value);
    }

  }

}
