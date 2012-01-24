/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.builtin.ArrayList;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Iterator;
import java.util.List;

public class NestedConcurrentLockTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return NestedConcurrentLockTestApp.class;
  }

  public static class NestedConcurrentLockTestApp extends AbstractErrorCatchingTransparentApp {
    private static final int NUM            = 1000;
    private final Object     concurrentLock = new Object();
    private final List       list           = new ArrayList();

    public NestedConcurrentLockTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      for (int i = 1; i <= NUM; i++) {
        concurrent();
        if ((i % 100) == 0) {
          System.out.println(getApplicationId() + " has reached " + i);
        }
      }
    }

    private void concurrent() {
      synchronized (concurrentLock) {
        write();
      }
    }

    private void write() {
      synchronized (list) {
        int add = validate();
        list.add(Integer.valueOf(add));
      }
    }

    private int validate() {
      int expect = 0;
      for (Iterator iter = list.iterator(); iter.hasNext();) {
        Integer integer = (Integer) iter.next();
        if (integer.intValue() != expect) { throw new RuntimeException("Expected " + expect + ", but was "
                                                                       + integer.intValue() + "\n" + list); }
        expect++;
      }

      return expect;
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = NestedConcurrentLockTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      String methodExpression = "* " + testClass + ".concurrent(..)";
      config.addAutolock(methodExpression, ConfigLockLevel.CONCURRENT);

      methodExpression = "* " + testClass + ".write(..)";
      config.addAutolock(methodExpression, ConfigLockLevel.WRITE);

      spec.addRoot("list", "list");
      spec.addRoot("concurrentLock", "concurrentLock");
    }

  }

}
