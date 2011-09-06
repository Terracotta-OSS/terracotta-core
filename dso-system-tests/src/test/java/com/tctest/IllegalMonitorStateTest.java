/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class IllegalMonitorStateTest extends TransparentTestBase {

  public void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(1).setIntensity(1);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    private static Object root1 = new Object();
    private static Object root2 = new Object();

    protected void runTest() throws Throwable {
      testMethods(root1);

      wrongLockHeld();
      readOnlyLockHeld();
      concurrentLockHeld();
    }

    private void wrongLockHeld() throws InterruptedException {
      // At the time of writing this test, there was a different error condition when the calling thread held at least
      // one lock before performing the bad monitor operation -- thus this code
      synchronized (root2) {
        assertTrue(ManagerUtil.isLocked(root2, Manager.LOCK_TYPE_WRITE));
        testMethods(root1);
      }
    }

    private void readOnlyLockHeld() throws InterruptedException {
      synchronized (root1) {
        assertTrue(ManagerUtil.isLocked(root1, Manager.LOCK_TYPE_READ));
        testMethods(root1);
      }
    }

    private void concurrentLockHeld() throws InterruptedException {
      synchronized (root1) {
        // Locking system no longer tracks concurrent holds
        //assertTrue(ManagerUtil.isLocked(root1, Manager.LOCK_TYPE_CONCURRENT));
        testMethods(root1);
      }
    }

    private void testMethods(Object o) throws InterruptedException {
      try {
        o.wait();
      } catch (IllegalMonitorStateException ise) {
        // expected
      }

      try {
        o.wait(500);
      } catch (IllegalMonitorStateException ise) {
        // expected
      }

      try {
        o.wait(1000, 42);
      } catch (IllegalMonitorStateException ise) {
        // expected
      }

      try {
        o.notify();
      } catch (IllegalMonitorStateException ise) {
        // expected
      }

      try {
        o.notifyAll();
      } catch (IllegalMonitorStateException ise) {
        // expected
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      config.addWriteAutolock("* " + testClass + ".wrongLockHeld()");
      config.addReadAutolock("* " + testClass + ".readOnlyLockHeld()");
      config.addAutolock("* " + testClass + ".concurrentLockHeld()", ConfigLockLevel.CONCURRENT);

      spec.addRoot("root1", "root1");
      spec.addRoot("root2", "root2");
    }

  }

}
