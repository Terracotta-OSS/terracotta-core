/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activeactive.ActiveActiveTestSetupManager;

public class LinkedBlockingQueueActiveActiveTest extends ActiveActiveTransparentTestBase {

  private static final int NODE_COUNT   = 1;
  private final int        electionTime = 5;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  public void setupActiveActiveTest(ActiveActiveTestSetupManager setupManager) {
    setupManager.setServerCount(4);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.DISK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
    setupManager.addActiveServerGroup(2, MultipleServersSharedDataMode.DISK, electionTime);
    setupManager.addActiveServerGroup(2, MultipleServersSharedDataMode.DISK, electionTime);
  }
}
