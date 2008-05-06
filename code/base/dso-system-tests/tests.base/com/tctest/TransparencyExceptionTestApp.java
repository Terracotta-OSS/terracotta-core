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

import java.util.HashMap;
import java.util.Map;

/**
 * An app that throws an exception in a lock method and makes sure things still work ok
 */
public class TransparencyExceptionTestApp  extends AbstractTransparentApp {
  private Map myRoot = new HashMap();
  private boolean fail = true;

  public TransparencyExceptionTestApp (String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec("com.tctest.TransparencyExceptionTestApp");
    spec.addRoot("myRoot", "rootBabyRoot");
    String methodExpression = "void com.tctest.TransparencyExceptionTestApp.test1()";
    config.addWriteAutolock(methodExpression);
  }
  
  public void run() {
    test();
    fail = false;
    test();
  }

  public void test() {
    try {
      test1();
    } catch (AssertionError e) {
      if(fail)  {
        System.out.println("SUCCESS");
      } else {
        throw new AssertionError("Failed !!");
      }
      return;
    }
    if(fail) {
      throw new AssertionError("Failed !!");
    } else {
        System.out.println("SUCCESS");
    }
  }

  public void test1() {
    synchronized (myRoot) {
      myRoot.put(new Long(1), new Long(1));
      if(fail) throw new AssertionError("Testing one two three");
    }
  }

}