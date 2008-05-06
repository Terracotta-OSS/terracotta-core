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

public class DuplicateRootNameTestApp extends AbstractTransparentApp {
  private Integer        intObjRoot;        // testing duplicate root name, so this varilable is not being used.
  private Long           longObjRoot;       // testing duplicate root name, so this varilable is not being used.

  private int            intRoot;           // testing duplicate root name, so this varilable is not being used.
  private long           longRoot;          // testing duplicate root name, so this varilable is not being used.

  private static Integer staticIntObjRoot;  // testing duplicate root name, so this varilable is not being used.
  private static Long    staticLongObjRoot; // testing duplicate root name, so this varilable is not being used.

  private static int     staticIntRoot;     // testing duplicate root name, so this varilable is not being used.
  private static long    staticLongRoot;    // testing duplicate root name, so this varilable is not being used.

  public DuplicateRootNameTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    intObjRoot = new Integer(10);
    try {
      longObjRoot = new Long(100);
      System.out.println(intObjRoot);   // Putting this println to get rid of the eclipse warnings.
      System.out.println(longObjRoot);  // Putting this println to get rid of the eclipse warnings.
      throw new AssertionError("Should have thrown a ClassCastException due to duplicate root name");
    } catch (ClassCastException e) {
      // Expected.
    }

    intRoot = 10;
    try {
      longRoot = 100L;
      System.out.println(intRoot);  // Putting this println to get rid of the eclipse warnings.
      System.out.println(longRoot); // Putting this println to get rid of the eclipse warnings.
      throw new AssertionError("Should have thrown a ClassCastException due to duplicate root name");
    } catch (ClassCastException e) {
      // Expected.
    }

    staticIntObjRoot = new Integer(10);
    try {
      staticLongObjRoot = new Long(100);
      System.out.println(staticIntObjRoot);  // Putting this println to get rid of the eclipse warnings.
      System.out.println(staticLongObjRoot); // Putting this println to get rid of the eclipse warnings.
      throw new AssertionError("Should have thrown a ClassCastException due to duplicate root name");
    } catch (ClassCastException e) {
      // Expected.
    }

    staticIntRoot = 10;
    try {
      staticLongRoot = 100;
      System.out.println(staticIntRoot);  // Putting this println to get rid of the eclipse warnings.
      System.out.println(staticLongRoot); // Putting this println to get rid of the eclipse warnings.
      throw new AssertionError("Should have thrown a ClassCastException due to duplicate root name");
    } catch (ClassCastException e) {
      // Expected.
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = DuplicateRootNameTestApp.class.getName();

    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("intObjRoot", "objRoot");
    spec.addRoot("longObjRoot", "objRoot");
    spec.addRoot("intRoot", "primitiveRoot", true);
    spec.addRoot("longRoot", "primitiveRoot", true);
    spec.addRoot("staticIntObjRoot", "staticRoot");
    spec.addRoot("staticLongObjRoot", "staticRoot");
    spec.addRoot("staticIntRoot", "staticPrimitiveRoot", true);
    spec.addRoot("staticLongRoot", "staticPrimitiveRoot", true);
  }

}
