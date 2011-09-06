/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.objectserver;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.runtime.Os;
import com.tctest.YoungGCTestAndActivePassiveTestBase;

import java.util.ArrayList;
import java.util.Date;

public class CreateRescueCandidatesYoungGCTest extends YoungGCTestAndActivePassiveTestBase {
  private final long LOW_FREE_MEMORY    = 20 * 1024 * 1024;
  private final long MIDDLE_FREE_MEMORY = 40 * 1024 * 1024;
  private final int  LOW_APP_NODES      = 1;
  private final int  MIDDLE_APP_NODES   = 2;
  private final int  HIGH_APP_NODES     = 3;

  public CreateRescueCandidatesYoungGCTest() {
    if (Runtime.getRuntime().availableProcessors() < 2) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  // force to use external process to run normal mode
  @Override
  protected boolean useExternalProcess() {
    if (isRunNormalMode()) {
      return true;
    } else {
      return super.useExternalProcess();
    }
  }

  @Override
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);

    // set client instance according to available free memory
    long freemem = Runtime.getRuntime().freeMemory();
    int app_count;
    if (Os.isSolaris() || Os.isWindows() || freemem < LOW_FREE_MEMORY) {
      app_count = LOW_APP_NODES;
    } else if (freemem < MIDDLE_FREE_MEMORY) {
      app_count = MIDDLE_APP_NODES;
    } else {
      app_count = HIGH_APP_NODES;
    }
    gcConfigHelper.setNodeCount(app_count);

    jvmArgs.add("-verbose:gc");
    jvmArgs.add("-XX:+PrintGCTimeStamps");
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  protected Class getApplicationClass() {
    return CreateRescueCandidatesYoungGCTestApp.class;
  }

  protected int getGarbageCollectionInterval() {
    return 180;
  }

}
