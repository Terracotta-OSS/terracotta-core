/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

public class ShareCurrencyTestApp extends AbstractErrorCatchingTransparentApp {
  private final CyclicBarrier barrier;
  private final Map           mapRoot = new HashMap();

  public ShareCurrencyTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, com.tc.object.config.DSOClientConfigHelper config) {
    String testClass = ShareCurrencyTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("barrier", "barrier");
    spec.addRoot("mapRoot", "mapRoot");
    new CyclicBarrierSpec().visit(visitor, config);

  }

  protected void runTest() throws Throwable {
    int index = barrier.barrier();

    if (index == 0) {
      synchronized (mapRoot) {
        mapRoot.put("EUR", Currency.getInstance("EUR"));
      }
    }

    barrier.barrier();

    synchronized (mapRoot) {
      Currency cur = (Currency) mapRoot.get("EUR");

      Assert.assertTrue(cur == Currency.getInstance("EUR"));
    }
  }

}
