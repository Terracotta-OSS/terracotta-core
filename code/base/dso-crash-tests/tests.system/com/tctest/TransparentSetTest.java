/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class TransparentSetTest extends TransparentTestBase implements TestConfigurator {
  private static final int NODE_COUNT           = 3;
  private static final int EXECUTION_COUNT      = 3;
  private static final int LOOP_ITERATION_COUNT = 600;

  protected Class getApplicationClass() {
    return TransparentSetTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }
  
  protected boolean canRunActivePassive() {
    return true;
  }
  
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveTestSetupManager.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveTestSetupManager.DISK);
    setupManager.setServerPersistenceMode(ActivePassiveTestSetupManager.PERMANENT_STORE);
  }

}
