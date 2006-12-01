/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;

  private final DataRoot      root = new DataRoot();

  public AtomicIntegerTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
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
      root.getIntValue().set(10);
    }

    barrier.barrier();

  }

  private void basicSetTesting() throws Exception {
    clear();
    initialize();

    Assert.assertEquals(10, root.getIntValue().get());

    barrier.barrier();
  }

  private void basicGetTesting() throws Exception {
    clear();
    initialize();

    Assert.assertEquals(10, root.getIntValue().get());
    Assert.assertEquals(10.0D, root.getIntValue().doubleValue());
    Assert.assertEquals((byte) 10, root.getIntValue().byteValue());
    Assert.assertEquals(10.0f, root.getIntValue().floatValue());
    Assert.assertEquals(10, root.getIntValue().intValue());
    Assert.assertEquals(10L, root.getIntValue().longValue());
    Assert.assertEquals(10, root.getIntValue().shortValue());

    barrier.barrier();
  }

  private void addAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();

    if (index == 0) {
      int val = root.getIntValue().addAndGet(4);
      Assert.assertEquals(14, val);
    }

    barrier.barrier();

    Assert.assertEquals(14, root.getIntValue().get());

    barrier.barrier();
  }

  private void compareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getIntValue().compareAndSet(10, 18);
    }

    barrier.barrier();

    Assert.assertEquals(18, root.getIntValue().get());

    barrier.barrier();
  }

  private void weakCompareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getIntValue().weakCompareAndSet(10, 20);
    }

    barrier.barrier();

    Assert.assertEquals(20, root.getIntValue().get());

    barrier.barrier();
  }

  private void getAndIncrementTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().getAndIncrement();
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(11, root.getIntValue().get());

    barrier.barrier();
  }

  private void getAndDecrementTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().getAndDecrement();
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(9, root.getIntValue().get());

    barrier.barrier();
  }

  private void getAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().getAndSet(200);
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(200, root.getIntValue().get());

    barrier.barrier();
  }

  private void getAndAddTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().getAndAdd(5);
      Assert.assertEquals(10, val);
    }

    barrier.barrier();

    Assert.assertEquals(15, root.getIntValue().get());

    barrier.barrier();
  }

  private void incrementAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().incrementAndGet();
      Assert.assertEquals(11, val);
    }

    barrier.barrier();

    Assert.assertEquals(11, root.getIntValue().get());

    barrier.barrier();
  }

  private void decrementAndGetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      int val = root.getIntValue().decrementAndGet();
      Assert.assertEquals(9, val);
    }

    barrier.barrier();

    Assert.assertEquals(9, root.getIntValue().get());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = AtomicIntegerTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
  }

  private static class DataRoot {
    private AtomicInteger intValue = new AtomicInteger(0);

    public DataRoot() {
      super();
    }

    public AtomicInteger getIntValue() {
      return intValue;
    }

    public void setIntValue(AtomicInteger intValue) {
      this.intValue = intValue;
    }

    public void clear() {
      intValue.set(0);
    }
  }
}
