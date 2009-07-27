/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.runner.TransparentAppConfig;

public class ReentrantReadWriteLockCrashTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig appConfig = t.getTransparentAppConfig();
    appConfig.setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    appConfig.setAttribute(ReentrantReadWriteLockTestApp.CRASH_TEST, "true");
  }

  @Override
  protected Class getApplicationClass() {
    return ReentrantReadWriteLockTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
