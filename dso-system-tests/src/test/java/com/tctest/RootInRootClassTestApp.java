/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RootInRootClassTestApp extends AbstractTransparentApp {

  private final Map myInnerInMap;

  public RootInRootClassTestApp() {
    super("", new TransparentAppConfig(null, null, 0, 0, null), null);
    Map m = new HashMap();
    m.put(Long.valueOf(System.currentTimeMillis()), new TestObject());
    myInnerInMap = m;

    System.out.println("CALLED NO ARG CONST:");
    new Error().printStackTrace();
  }

  public RootInRootClassTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    Map m = new HashMap();
    m.put(Long.valueOf(System.currentTimeMillis()), new TestObject());
    myInnerInMap = m;
  }

  public int getMapSize() {
    synchronized (myInnerInMap) {
      return myInnerInMap.size();
    }
  }

  public void run() {

    try {
      synchronized (myInnerInMap) {
        myInnerInMap.put("test" + myInnerInMap.size(), new TestObject());
        System.out.println("myInnerMap size:" + myInnerInMap.size());
        for (Iterator i = myInnerInMap.values().iterator(); i.hasNext();) {
          TestObject to = (TestObject) i.next();
          to.doStuff();
        }
      }
      Thread.sleep(1000);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public class TestObject {

    public void doStuff() {
      System.out.println("myInner:" + getMapSize());
    }
    // blah
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = RootInRootClassTestApp.class.getName();
    config.addIncludePattern(testClass, false);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.getOrCreateSpec(testClass).addRoot("myInnerInMap", "myInnerInMap");

    config.addIncludePattern(TestObject.class.getName());
  }

}
