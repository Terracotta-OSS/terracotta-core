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
import com.tctest.runner.AbstractTransparentApp;

import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;

  private final DataRoot      root = new DataRoot();

  public AtomicBooleanTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      AtomicBoolean ab = new AtomicBoolean();
      Assert.assertTrue(ab instanceof Manageable);
      atomicBooleanTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void atomicBooleanTesting() throws Exception {
    basicSetTesting();
    compareAndSetTesting();
    weakCompareAndSetTesting();
    getAndSetTesting();
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
      root.getBoolValue().set(true);
    }

    barrier.barrier();

  }

  private void basicSetTesting() throws Exception {
    clear();
    initialize();

    Assert.assertEquals(true, root.getBoolValue().get());

    barrier.barrier();
  }

  private void compareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getBoolValue().compareAndSet(true, false);
    }

    barrier.barrier();

    Assert.assertEquals(false, root.getBoolValue().get());

    barrier.barrier();
  }

  private void weakCompareAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      root.getBoolValue().weakCompareAndSet(true, false);
    }

    barrier.barrier();

    Assert.assertEquals(false, root.getBoolValue().get());

    barrier.barrier();
  }

  private void getAndSetTesting() throws Exception {
    clear();
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      boolean val = root.getBoolValue().getAndSet(false);
      Assert.assertEquals(true, val);
    }

    barrier.barrier();

    Assert.assertEquals(false, root.getBoolValue().get());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = AtomicBooleanTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(testClass + "$*");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
  }

  private static class DataRoot {
    private final AtomicBoolean boolValue = new AtomicBoolean();

    public DataRoot() {
      super();
    }

    public AtomicBoolean getBoolValue() {
      return boolValue;
    }

    public void clear() {
      boolValue.set(false);
    }
  }
}
