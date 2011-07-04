/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.List;

public class ByteCodePerformanceTestApp extends AbstractTransparentApp {

  private List             instrumentedObjects         = new ArrayList();
  private List             instrumentedUnSharedObjects = new ArrayList();
  // private List uninstrumentedObjects = new ArrayList();

  private final static int COUNT                       = 50;

  public ByteCodePerformanceTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    new InstrumentedObject(true);
    long start = System.currentTimeMillis();
    long s2 = 0;
    if (true) {
      synchronized (this.instrumentedObjects) {
        s2 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
          instrumentedObjects.add(new InstrumentedObject(true));
        }
        System.out.println("CREATED INSTRUMENTED -INNER:" + (System.currentTimeMillis() - s2));
      }

      System.out.println("CREATED INSTRUMENTED:" + (System.currentTimeMillis() - start));

      start = System.currentTimeMillis();
      System.gc();
      synchronized (this.instrumentedObjects) {
        System.out.println("QUERY INSTRUMENTED START:" + (System.currentTimeMillis() - start));
        for (int j = 0; j < 500; j++) {
          for (int i = 0; i < COUNT; i++) {
            InstrumentedObject o = (InstrumentedObject) instrumentedObjects.get(i);
            o.accessValues();
          }
        }
      }
      System.out.println("QUERY INSTRUMENTED:" + (System.currentTimeMillis() - start));
    } else {
      // start = System.currentTimeMillis();
      // synchronized (this.instrumentedObjects) {
      // for (int i = 0; i < COUNT; i++) {
      // InstrumentedObject o = (InstrumentedObject) instrumentedObjects.get(i);
      // o.setValues();
      // }
      // }
      // System.out.println("SET INSTRUMENTED:" + (System.currentTimeMillis() - start));
      //
      start = System.currentTimeMillis();
      for (int i = 0; i < COUNT; i++) {
        instrumentedUnSharedObjects.add(new InstrumentedObject(true));
      }
      System.out.println("CREATED INSTRUMENTED UNSHARED:" + (System.currentTimeMillis() - start));
      System.gc();
      start = System.currentTimeMillis();
      for (int j = 0; j < 500; j++) {
        for (int i = 0; i < COUNT; i++) {
          InstrumentedObject o = (InstrumentedObject) instrumentedUnSharedObjects.get(i);
          o.accessValues();
        }
      }
      System.out.println("QUERY INSTRUMENTED UNSHARED:" + (System.currentTimeMillis() - start));
    }
    //
    // start = System.currentTimeMillis();
    // for (int j = 0; j < 500; j++) {
    // for (int i = 0; i < COUNT; i++) {
    // InstrumentedObject o = (InstrumentedObject) instrumentedUnSharedObjects.get(i);
    // o.setValues();
    // }
    // }
    // System.out.println("SET INSTRUMENTED UNSHARED:" + (System.currentTimeMillis() - start));
    //
    // start = System.currentTimeMillis();
    // for (int i = 0; i < COUNT; i++) {
    // uninstrumentedObjects.add(new UnInstrumentedObject(true));
    // }
    // System.out.println("CREATED UNINSTRUMENTED:" + (System.currentTimeMillis() - start));
    //
    // start = System.currentTimeMillis();
    // for (int j = 0; j < 500; j++) {
    // for (int i = 0; i < COUNT; i++) {
    // UnInstrumentedObject o = (UnInstrumentedObject) uninstrumentedObjects.get(i);
    // o.accessValues();
    // }
    // }
    // System.out.println("QUERY UNINSTRUMENTED:" + (System.currentTimeMillis() - start));
    //
    // start = System.currentTimeMillis();
    // for (int j = 0; j < 500; j++) {
    // for (int i = 0; i < COUNT; i++) {
    // UnInstrumentedObject o = (UnInstrumentedObject) uninstrumentedObjects.get(i);
    // o.setValues();
    // }
    // }
    // System.out.println("SET UNINSTRUMENTED:" + (System.currentTimeMillis() - start));

  }

  public List test() {
    if (System.currentTimeMillis() == 100) {
      synchronized (this) {

        boolean steve = true;
        System.out.println(steve);
        return instrumentedObjects;
      }
    }
    return instrumentedObjects;

  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = InstrumentedObject.class.getName();
    config.getOrCreateSpec(testClass);
    testClass = ByteCodePerformanceTestApp.class.getName();
    String methodExpression = "* " + testClass + ".*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    spec.addRoot("instrumentedObjects", "instrumentedObjects");
  }
}
