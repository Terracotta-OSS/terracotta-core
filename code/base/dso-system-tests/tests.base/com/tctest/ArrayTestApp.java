/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Random;

public class ArrayTestApp extends AbstractTransparentApp {

  private String[] myArrayTestRoot;

  public ArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.myArrayTestRoot = new String[] { "hee", "hoo", "haa" };
  }

  public void run() {
    Random rand = new Random();
    try {
      synchronized (myArrayTestRoot) {
        System.out.println(myArrayTestRoot[rand.nextInt(myArrayTestRoot.length)]);
      }
      Thread.sleep(1);
    } catch (Exception e) {
      e.printStackTrace();
    }

    arrayIndexTestCase();

    testNullArrayAccess();
  }

  private void testNullArrayAccess() {
    Object[] o = null;

    try {
      if (o[3] == null) { throw new AssertionError(); }
    } catch (NullPointerException npe) {
      // expecte
    }
  }

  private void arrayIndexTestCase() {
    // We had a bug where ArrayIndexOutOfBoundsException failed to release a monitor, this is the test case for it
    try {
      for (int i = 0; true; i++) {
        Object o = myArrayTestRoot[i];

        // silence warning about unread local variable
        if (o == null) {
          continue;
        }
      }
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //
    }

    try {
      Object o = myArrayTestRoot[-1];
      if (true || o == o) { throw new AssertionError(); }
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //
    }

  }

  public void setArray(String[] blah) {
    myArrayTestRoot = blah;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ArrayTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("myArrayTestRoot", "myArrayTestRoot");

  }
}
