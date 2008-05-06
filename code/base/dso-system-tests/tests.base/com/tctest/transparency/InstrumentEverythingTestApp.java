/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.transparency;

import org.apache.commons.collections.FastHashMap;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class InstrumentEverythingTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int INITIAL      = 0;
  private static final int INTERMEDIATE = 1;
  private static final int END          = 2;

  final List               root         = new ArrayList();
  final CyclicBarrier      barrier;

  public InstrumentEverythingTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = InstrumentEverythingTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("root", "root");
    spec.addRoot("barrier", "barrier");

    CyclicBarrierSpec cbspec = new CyclicBarrierSpec();
    cbspec.visit(visitor, config);

    // config.addExcludePattern("*..SubClassA");

    // Include everything to be instrumented.
    config.addIncludePattern("*..*", false);
  }

  public void runTest() throws BrokenBarrierException, InterruptedException {
    int n = barrier.barrier();
    moveToStage(INITIAL);
    if (n == 0) {
      synchronized (root) {
        addALotOfObjects(root);
      }
      moveToStage(INTERMEDIATE);
    } else {
      List local = new ArrayList();
      addALotOfObjects(local);
      moveToStageAndWait(INTERMEDIATE);
      synchronized (root) {
        verify(local, root);
      }
    }
    printDetails();
    moveToStage(END);
  }

  private void verify(List expected, List actual) {
    Assert.assertEquals(expected, actual);
  }

  private void addALotOfObjects(List l) {
    HashMap map = new HashMap();
    map.put("hello", "saro");
    map.put(new Integer(10), new Vector());
    l.add(map);
    l.add(new ArrayList());
    l.add(new SubClassA());
    l.add(new SubClassB());
    l.add(new SubClassB());
    l.add(new SubClassC());
    l.add(new SubClassD());
    // l.add(new StringBuffer("hello there"));
    addFastHashMaps(l);
    addClonedObjects(l);
  }

  private void addFastHashMaps(List l) {
    // This is added to test fasthashmap's clone method. It uses clone() to put() objects in the
    // map and earlier this used to cause problem. esp. when fast=true !!
    FastHashMap fslow = new FastHashMap();
    l.add(fslow);
    fslow.put("key1", "value1");
    fslow.put("key2", "value2");
    fslow.put("key3", "value3");
    fslow.put("key4", "value4");
    fslow.put("key5", "value5");

    FastHashMap freallyslow = new FastHashMap();
    l.add(freallyslow);
    freallyslow.setFast(true);
    freallyslow.put("key1", "value1");
    freallyslow.put("key2", "value2");
    freallyslow.put("key3", "value3");
    freallyslow.put("key4", "value4");
    freallyslow.put("key5", "value5");
  }

  // test to make sure clonedObjects gets shared properly.
  private void addClonedObjects(List l) {
    SubClassA a = new SubClassA();
    synchronizedAdd(l, a);
    synchronizedAdd(l, a.getCopy());
    SubClassB b = new SubClassB();
    b.method1();
    synchronizedAdd(l, b);
    synchronizedAdd(l, b.getCopy());
    SubClassC c = new SubClassC();
    synchronizedAdd(l, c);
    synchronizedAdd(l, c);
    synchronizedAdd(l, c);
    synchronizedAdd(l, c.getCopy());
    synchronizedAdd(l, c.getCopy());
    synchronizedAdd(l, c.clone());
    synchronizedAdd(l, c.clone());
    synchronizedAdd(l, c.clone());
    SubClassD d = new SubClassD();
    synchronizedAdd(l, d.clone());
    synchronizedAdd(l, d.clone());
    synchronizedAdd(l, d);
    synchronizedAdd(l, d);
    synchronizedAdd(l, d.clone());
    synchronizedAdd(l, d.clone());
  }

  private void synchronizedAdd(List l, Object o) {
    synchronized (l) {
      l.add(o);
    }
  }

  private void printDetails() {
    synchronized (root) {
      System.err.println(Thread.currentThread().getName() + ": Root size() = " + root.size());
    }
  }

}
