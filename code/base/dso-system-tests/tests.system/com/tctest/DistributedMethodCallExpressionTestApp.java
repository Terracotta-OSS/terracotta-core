/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

/**
 * This test is to test the various method expressions for DistributedMethodCall. The test will also make sure that
 * constructors and static methods will not be called.
 */
public class DistributedMethodCallExpressionTestApp extends AbstractTransparentApp {

  private SharedModel model = new SharedModel();

  public DistributedMethodCallExpressionTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    callNonStaticMethod();
    callStaticMethod();
  }

  public void callNonStaticMethod() {
    moveToStageAndWait(1);
    boolean called = false;
    synchronized (model) {
      if (System.getProperty("calledNonStaticMethod") == null) {
        model.nonStaticMethod(null, 0, 0, null, null, false);
        called = true;
      }
    }
    ThreadUtil.reallySleep(10000);
    if (called) {
      int i = Integer.parseInt((System.getProperty("calledNonStaticMethod")));
      if (i != 3) {
        notifyError("Wrong number of calls:" + i);
      }
    }
  }
  
  public void callStaticMethod() {
    moveToStageAndWait(1);
    boolean called = false;
    synchronized (model) {
      if (System.getProperty("callStaticMethod") == null) {
        SharedModel.staticMethod();
        called = true;
      }
    }
    ThreadUtil.reallySleep(10000);
    if (called) {
      int i = Integer.parseInt((System.getProperty("callStaticMethod")));
      if (i != 1) {
        notifyError("Wrong number of calls:" + i);
      }
    }
  }


  public static class SharedModel {

    public static void staticMethod() {
      synchronized (System.getProperties()) {
        String property = System.getProperty("callStaticMethod");
        int num = 0;

        if (property != null) {
          num = Integer.parseInt(property);
        }

        System.setProperty("callStaticMethod", Integer.toString(++num));
      }
    }

    public void nonStaticMethod(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b) {
      synchronized (System.getProperties()) {
        String property = System.getProperty("calledNonStaticMethod");
        int num = 0;
        if (property != null) {
          num = Integer.parseInt(property);
        }

        System.setProperty("calledNonStaticMethod", Integer.toString(++num));
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      TransparencyClassSpec spec = config.getOrCreateSpec(FooObject.class.getName());
      String testClassName = DistributedMethodCallExpressionTestApp.class.getName();
      spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("model", "model");
      String methodExpression = "* " + testClassName + "*.*(..)";
      System.err.println("Adding autolock for: " + methodExpression);
      config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedModel.class.getName());
      spec.addDistributedMethodCall("staticMethod", "()V");
      try {
        spec.addDistributedMethodCall("<init>", "()V");
        throw new AssertionError("Should have thrown an AssertionError.");
      } catch (AssertionError e) {
        // Expected.
      }
      config.addDistributedMethodCall("* com.tctest.DistributedMethodCallExpressionTestApp$SharedModel.nonStaticMethod(..)");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
