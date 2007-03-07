/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.List;

public class MissingClassApplyTestApp extends AbstractTransparentApp {

  private static final String MISSING_CLASS_NAME = MissingClassApplyTestApp.class.getName() + "$MissingClass";
  private List                root               = new ArrayList();
  private CyclicBarrier       barrier            = new CyclicBarrier(getParticipantCount());

  public MissingClassApplyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    System.err.println("\n### CTor.missingClass=" + MISSING_CLASS_NAME);
  }

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }

  }

  private void runTest() throws Exception {

    final int nodeId = barrier.barrier();
    final boolean masterNode = nodeId == 0;
    System.err.println("\n### NodeId=" + nodeId + ", ThreadId=" + Thread.currentThread().getName());
    if (!masterNode) {
      final IsolationClassLoader icl = (IsolationClassLoader) getClass().getClassLoader();
      icl.throwOnLoad(MISSING_CLASS_NAME, MISSING_CLASS_NAME + " should not be found in this node [nodeId=" + nodeId  + "]");
    }
    barrier.barrier();
    // make sure that referencing MissingClass throws exception everywhere except the master node...
    checkClassAvailability(masterNode);
    
    barrier.barrier();
    // make sure that we can push some updates to root
    if (masterNode) {
      add(new Object());
    }
    barrier.barrier();
    checkSize(1);
    barrier.barrier();
    // make sure that update that refer to MissingClass don't throw exceptions in those nodes that don't have access to it
    if (masterNode) {
      add(new MissingClass());
    }
    barrier.barrier();
    checkSize((masterNode) ? 2 : 1);
    barrier.barrier();
    // just for fun, check class availability again
    checkClassAvailability(masterNode);
  }

  private void checkClassAvailability(final boolean masterNode) {
    if (masterNode) {
      new MissingClass();
    } else {
      Throwable exception = null;
      try {
        new MissingClass();
      } catch (Throwable e) {
        exception = e;
      }
      if (exception == null) {
        notifyError("Expected exception was not thrown!");
      } 
    }
  }

  public void add(Object o) {
    synchronized (root) {
      root.add(o);
    }
  }

  public synchronized void checkSize(final int i) {
    int actual;
    synchronized (root) {
      actual = root.size();
    }
    if (i != actual) {
      notifyError("Unexpected size: expected=" + i + ", actual=" + actual);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MissingClassApplyTestApp.class.getName();

    System.err.println("\n### testClass=" + testClass);
    System.err.println("\n### missingClass=" + MISSING_CLASS_NAME);

    config.getOrCreateSpec(testClass) //
        .addRoot("barrier", "barrier") //
        .addRoot("root", "root");

    config.getOrCreateSpec(MISSING_CLASS_NAME);

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    new CyclicBarrierSpec().visit(visitor, config);
  }

  static class MissingClass {
    // n/a
  }

}
