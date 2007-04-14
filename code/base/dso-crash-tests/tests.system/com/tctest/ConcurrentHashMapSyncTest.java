/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class ConcurrentHashMapSyncTest extends TransparentTestBase {

  // need to be at least 4
  private static final int NODE_COUNT = 8;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ConcurrentHashMapSyncTestApp.class;
  }
  

  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveTestSetupManager.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitInSec(15);
    setupManager.setServerShareDataMode(ActivePassiveTestSetupManager.DISK);
    setupManager.setServerPersistenceMode(ActivePassiveTestSetupManager.PERMANENT_STORE);
  }



}
