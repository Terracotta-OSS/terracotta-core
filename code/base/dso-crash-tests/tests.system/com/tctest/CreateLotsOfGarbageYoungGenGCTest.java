/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;


import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.runtime.Os;

import java.util.Date;

public class CreateLotsOfGarbageYoungGenGCTest extends YoungGCTestBase implements TestConfigurator {

  public CreateLotsOfGarbageYoungGenGCTest() {
    if (Os.isSolaris()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  protected Class getApplicationClass() {
    return CreateLotsOfGarbageGCTestApp.class;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  // start only 1 L1
  protected int getNodeCount() {
    return 1;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
