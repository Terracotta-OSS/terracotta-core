/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

import java.util.HashSet;
import java.util.Set;

public class LiteralAutoLockTestApp extends AbstractTransparentApp {
  private Set           nodes   = new HashSet();
  private CyclicBarrier barrier = new CyclicBarrier(2);

  public LiteralAutoLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    int size = 0;
    synchronized ("Steve") {
      nodes.add(new Object());
      size = nodes.size();
    }
    try {
      System.out.println("barrier:" + size);
      barrier.barrier();
      System.out.println("barrier out:" + size);
    } catch (InterruptedException ie) {
      notifyError(ie);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = LiteralAutoLockTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("nodes", "nodes");
    spec.addRoot("barrier", "barrier");
    new CyclicBarrierSpec().visit(visitor, config);
  }
}
