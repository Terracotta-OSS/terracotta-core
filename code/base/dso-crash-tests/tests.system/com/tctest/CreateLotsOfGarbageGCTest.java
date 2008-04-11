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
import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.runner.AbstractTransparentApp;

public class CreateLotsOfGarbageGCTest extends GCTestBase implements TestConfigurator {

  protected Class getApplicationClass() {
    return CreateLotsOfGarbageGCTestApp.class;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public int getGarbageCollectionInterval() {
    return 100;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(3);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(120);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  /**
   * Test Application
   */
  public static class CreateLotsOfGarbageGCTestApp extends AbstractTransparentApp {

    private static final int SIZE       = 100;
    private static final int LOOP_COUNT = 8000;

    private Object[]         array      = new Object[SIZE];

    public CreateLotsOfGarbageGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      String testClass = CreateLotsOfGarbageGCTestApp.class.getName();
      TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

      String methodExpression = "* " + testClass + "*.*(..)";
      config.addWriteAutolock(methodExpression);

      spec.addRoot("array", "root");
    }

    public void run() {
      for (int i = 0; i < LOOP_COUNT; i++) {
        synchronized (array) {
          for (int j = 0; j < SIZE; j++) {
            array[j] = new Object();
          }
        }
        if (i != 0 && i % 100 == 0) {
          System.out.println("Loop count : " + i);
        }
      }
    }
  }
}
