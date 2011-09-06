/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Random;

public class ArrayTestApp extends AbstractTransparentApp {

  private String[]          myArrayTestRoot;
  final private String[]    stringAry = { "hee", "hoo", "haa", "terracotta", "google", "yahoo", "apple" };
  final private static long runtime   = 1000 * 200;                                                       // 200

  // seconds

  public ArrayTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.myArrayTestRoot = new String[] { "hee", "hoo", "haa", "terracotta", "google", "yahoo", "apple" };
  }

  public void run() {
    Random rand = new Random();
    long end = System.currentTimeMillis() + runtime;
    try {
      while (end > System.currentTimeMillis()) {
        synchronized (myArrayTestRoot) {
          int idx = rand.nextInt(myArrayTestRoot.length);
          // System.out.println(myArrayTestRoot[rand.nextInt(myArrayTestRoot.length)]);
          Assert.assertTrue(myArrayTestRoot[idx].equals(stringAry[idx]));
        }
        Thread.sleep((int) (Math.random() * 10));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    arrayIndexTestCase();

    testNullArrayAccess();
  }

  private void testNullArrayAccess() {
    Object[] o = returnNull();

    try {
      if (o[3] == null) { throw new AssertionError(); }
    } catch (NullPointerException npe) {
      // expected
    }
  }

  // This method is there to suppress Eclipse warning
  private Object[] returnNull() {
    return null;
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
      readArray(myArrayTestRoot, -1);
    } catch (ArrayIndexOutOfBoundsException aioobe) {
      //
    }

  }

  private static String readArray(String[] array, int i) {
    return array[i];
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
