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

import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceTestApp extends AbstractTransparentApp {

  // TC will "know" that these are intern()'d Strings since there is an explicit intern() call
  private static final String           TRUE  = "true".intern();
  private static final String           FALSE = "false".intern();

  private final AtomicReference<String> root = new AtomicReference<String>();
  private final CyclicBarrier           barrier;

  public AtomicReferenceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      AtomicReference ab = new AtomicReference();
      Assert.assertTrue(ab instanceof Manageable);
      atomicReferenceTesting();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void atomicReferenceTesting() throws Exception {
    basicSetTesting();
    compareAndSetTesting();
    weakCompareAndSetTesting();
    getAndSetTesting();
  }

  private void initialize() throws Exception {
    int index = barrier.barrier();

    if (index == 0) {
      root.set(TRUE);
    }

    barrier.barrier();
  }

  private void basicSetTesting() throws Exception {
    initialize();

    Assert.assertEquals(TRUE, root.get());

    barrier.barrier();
  }

  private void compareAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      boolean set = root.compareAndSet(TRUE, FALSE);
      if (!set) { throw new AssertionError("not set"); }
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  private void weakCompareAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      // per the javadoc this can fail spuriously. The implementations I've seen have no difference in compareAndSet()
      // and weakCompareAndSet(), but if this starts failing it is worth taking a second look
      boolean set = root.weakCompareAndSet(TRUE, FALSE);
      if (!set) { throw new AssertionError("not set"); }
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  private void getAndSetTesting() throws Exception {
    initialize();

    int index = barrier.barrier();
    if (index == 0) {
      Object val = root.getAndSet(FALSE);
      Assert.assertEquals(TRUE, val);
    }

    barrier.barrier();

    Assert.assertEquals(FALSE, root.get());

    barrier.barrier();
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = AtomicReferenceTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("root", "root");
  }

}
