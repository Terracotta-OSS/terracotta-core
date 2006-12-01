/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractTransparentApp;

import java.util.Arrays;

/**
 * TODO Mar 10, 2005: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public class DistributedMethodCallTestApp extends AbstractTransparentApp {

  private SharedModel model = new SharedModel();

  public DistributedMethodCallTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    moveToStageAndWait(1);
    boolean called = false;
    synchronized (model) {
      if (System.getProperty("called") == null) {
        System.setProperty("called", "0");

        FooObject[][] foos = new FooObject[2][3];
        for (int i = 0; i < foos.length; i++) {
          Arrays.fill(foos[i], new FooObject());
        }

        int count = 0;
        int[][][] ints = new int[6][8][9];
        for (int i = 0; i < ints.length; i++) {
          int[][] array1 = ints[i];
          for (int j = 0; j < array1.length; j++) {
            int[] array2 = array1[j];
            for (int k = 0; k < array2.length; k++) {
              array2[k] = count++;
            }
          }
        }

        model.addObject(new FooObject(), 1, 2, foos, ints, true);
        called = true;
      }
    }
    ThreadUtil.reallySleep(10000);
    if (called) {
      int i = Integer.parseInt((System.getProperty("called")));
      if (i != 3) {
        notifyError("Wrong number of calls:" + i);
      }

    }
  }

  public class SharedModel {
    public void addObject(Object obj, int i, double d, FooObject[][] foos, int[][][] ints, boolean b) {

      // Everything in the "foos" array should be non-null
      for (int index = 0; index < foos.length; index++) {
        FooObject[] array = foos[index];
        for (int j = 0; j < array.length; j++) {
          FooObject foo = array[j];
          Assert.assertNotNull(foo);
        }
      }

      // access all the "ints"
      int count = 0;
      for (int index = 0; index < ints.length; index++) {
        int[][] array1 = ints[index];
        for (int j = 0; j < array1.length; j++) {
          int[] array2 = array1[j];
          for (int k = 0; k < array2.length; k++) {
            int val = array2[k];
            Assert.assertEquals(count++, val);
          }
        }
      }

      if (obj == null || i != 1 || d != 2 || !b) {
        System.out.println("Invalid parameters:" + obj + " i:" + i + " d:" + d + " b:" + b);
        notifyError("Invalid parameters:" + obj + " i:" + i + " d:" + d + " b:" + b);
      } else {
        synchronized (System.getProperties()) {
          int num = Integer.parseInt(System.getProperty("called"));
          System.setProperty("called", Integer.toString(++num));
        }
      }
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    try {
      TransparencyClassSpec spec = config.getOrCreateSpec(FooObject.class.getName());
      String testClassName = DistributedMethodCallTestApp.class.getName();
      spec = config.getOrCreateSpec(testClassName);
      spec.addRoot("model", "model");
      spec.addRoot("sharedStuff", "sharedStuff");
      String methodExpression = "* " + testClassName + "*.*(..)";
      System.err.println("Adding autolock for: " + methodExpression);
      config.addWriteAutolock(methodExpression);

      spec = config.getOrCreateSpec(SharedModel.class.getName());
      spec.addDistributedMethodCall("addObject", "(Ljava/lang/Object;ID[[Lcom/tctest/FooObject;[[[IZ)V");
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
