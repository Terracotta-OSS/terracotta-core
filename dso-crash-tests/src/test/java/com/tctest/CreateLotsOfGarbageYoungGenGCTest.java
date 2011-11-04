/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.runtime.Os;

public class CreateLotsOfGarbageYoungGenGCTest extends YoungGCTestAndActivePassiveTestBase implements TestConfigurator {

  public CreateLotsOfGarbageYoungGenGCTest() {
    if (Os.isSolaris()) {
      disableTest();
    }
  }

  @Override
  protected Class getApplicationClass() {
    return CreateLotsOfGarbageGCTestApp.class;
  }

  // start only 1 L1
  protected int getNodeCount() {
    return 1;
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
