/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.bytecode.Manageable;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;

  private final DataRoot      root = new DataRoot();

  public AtomicLongTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      AtomicLong al = new AtomicLong();

      if (Vm.isIBM()) {
        Assert.assertFalse(al instanceof Manageable);
      } else {
        Assert.assertTrue(al instanceof Manageable);
      }

      atomicIntegerTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void atomicIntegerTesting() throws Exception {
    basicSetTesting();
    basicGetTesting();
    addAndGetTesting();
    compareAndSetTesting();
    weakCompareAndSetTesting();
    getAndIncrementTesting();
    getAndDecrementTesting();
    getAndSetTesting();
    getAndAddTesting();
    incrementAndGetTesting();
    decrementAndGetTesting();

    loadTest();

  }

  /**
   * This method was added to make it likely that some transactions on AtomicLong get folded (DEV-1439)
   */
  private void loadTest() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      for (int i = 0; i < 5000; i++) {
        root.getLongValue().incrementAndGet();
      }
    }

    barrier.barrier();

    Assert.assertEquals(5010, root.getLongValue().get());
  }

  private void clear() throws Exception {
    synchronized (root) {
      root.clear();
    }

    barrier.barrier();
  }

  private void initialize() throws Exception {

    int index = barrier.barrier();
    if (index == 0) {
      root.getLongValue().set(10);
    }

    barrier.barrier();
  }

  private void basicSetTesting() throws Exception {
    clear();
    initialize();

    Assert.assertEquals(10, root.getLongValue().get());

    barrier.barrier();
  }

  private void basicGetTesting() throws Exception {
    clear();
    initialize();

    Assert.assertEquals(10, root.getLongValue().get());
    Assert.assertEquals(10.0D, root.getLongValue().doubleValue());
    Assert.assertEquals((byte) 10, root.getLongValue().byteValue());
    Assert.assertEquals(10.0f, root.getLongValue().floatValue());
    Assert.assertEquals(10, root.getLongValue().intValue());
    Assert.assertEquals(10L, root.getLongValue().longValue());
    Assert.assertEquals(10, root.getLongValue().shortValue());

    barrier.barrier();
  }

  private void addAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().addAndGet(4);
      Assert.assertEquals(14, val);
    }

    barrier.barrier();

    Assert.assertEquals(14, root.getLongValue().get());

    barrier.barrier();
  }

  private void compareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getLongValue().compareAndSet(10, 18);
    }

    barrier.barrier();

    Assert.assertEquals(18, root.getLongValue().get());

    barrier.barrier();
  }

  private void weakCompareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getLongValue().weakCompareAndSet(10, 20);
    }

    barrier.barrier();

    Assert.assertEquals(20, root.getLongValue().get());

    barrier.barrier();
  }

  private void getAndIncrementTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().getAndIncrement();
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(11, root.getLongValue().get());

    barrier.barrier();
  }

  private void getAndDecrementTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().getAndDecrement();
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(9, root.getLongValue().get());

    barrier.barrier();
  }

  private void getAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().getAndSet(200);
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(200, root.getLongValue().get());

    barrier.barrier();
  }

  private void getAndAddTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().getAndAdd(5);
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(15, root.getLongValue().get());

    barrier.barrier();
  }

  private void incrementAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().incrementAndGet();
      Assert.assertEquals(11, val);
    }

    barrier.barrier();

    Assert.assertEquals(11, root.getLongValue().get());

    barrier.barrier();
  }

  private void decrementAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      long val = root.getLongValue().decrementAndGet();
      Assert.assertEquals(9, val);
    }

    barrier.barrier();

    Assert.assertEquals(9, root.getLongValue().get());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = AtomicLongTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
  }

  private static class DataRoot {
    private final AtomicLong longValue = new AtomicLong(0);

    public DataRoot() {
      super();
    }

    public AtomicLong getLongValue() {
      return longValue;
    }

    public void clear() {
      longValue.set(0);
    }
  }
}
