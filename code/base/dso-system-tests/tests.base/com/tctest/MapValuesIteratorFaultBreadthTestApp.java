/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapValuesIteratorFaultBreadthTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int COUNT = 10000;
  final Map                root  = new HashMap();
  CyclicBarrier            barrier;

  public MapValuesIteratorFaultBreadthTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MapValuesIteratorFaultBreadthTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");
    String methodExpression = "* " + testClass + ".read(..)";
    config.addReadAutolock(methodExpression);
    methodExpression = "* " + testClass + ".*(..)";
    config.addWriteAutolock(methodExpression);
    testClass = MapValuesIteratorFaultBreadthTestApp.Value.class.getName();
    spec = config.getOrCreateSpec(testClass);
    new CyclicBarrierSpec().visit(visitor, config);
  }

  protected void runTest() throws Throwable {
    setCyclicBarrier();
    faultRootMap();
    int count = barrier.barrier();
    if (count == 0) {
      fillUpRootMap();
      log("Population done");
      barrier.barrier();
    } else {
      barrier.barrier();
      faultValuesInMap();
    }
  }

  private void faultValuesInMap() {
    synchronized (root) {
      long start = System.currentTimeMillis();
      Iterator i = root.values().iterator();
      while (i.hasNext()) {
        Object v = i.next();
        if (v == null) { throw new AssertionError("Value null for " + i); }
      }
      log("Faulting doing in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  private void fillUpRootMap() {
    for (int i = 0; i < COUNT; i++) {
      synchronized (root) {
        root.put("Key-" + i, new Value());
      }
    }
  }

  private void faultRootMap() {
    for (int i = 0; i < 10; i++) {
      synchronized (root) {
        if (i == 9) {
          root.clear();
        } else {
          root.put("Init" + i, "Hello");
        }
      }
    }

  }

  private void setCyclicBarrier() {
    int participationCount = getParticipantCount();
    log("Participation Count = " + participationCount);
    barrier = new CyclicBarrier(participationCount);
  }

  static DateFormat formatter = new SimpleDateFormat("hh:mm:ss,S");

  private static void log(String message) {
    System.err.println(Thread.currentThread().getName() + " :: "
                       + formatter.format(new Date(System.currentTimeMillis())) + " : " + message);
  }

  private class Value {
    long time = System.currentTimeMillis();

    public String toString() {
      return "Value [ " + formatter.format(new Date(time)) + "]";
    }
  }

}
