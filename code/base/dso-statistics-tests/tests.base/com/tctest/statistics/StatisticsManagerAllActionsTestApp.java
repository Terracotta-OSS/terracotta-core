/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

public class StatisticsManagerAllActionsTestApp extends AbstractTransparentApp {

  public static final int NODE_COUNT = 2;

  private CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

  public StatisticsManagerAllActionsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    barrier();
  }

  private void barrier() {
    try {
      barrier.barrier();
    } catch (InterruptedException ie) {
      throw new AssertionError();
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = StatisticsManagerAllActionsTestApp.class.getName();

    config.getOrCreateSpec(testClass)
      .addRoot("barrier", "barrier");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    new CyclicBarrierSpec().visit(visitor, config);
  }
}