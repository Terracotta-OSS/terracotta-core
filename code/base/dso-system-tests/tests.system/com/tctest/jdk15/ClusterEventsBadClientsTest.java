/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TransparentTestIface;

/**
 * DEV-5051: Whenever a client connects to the cluster or disconnects, corresponding cluster events are fired and the
 * client's client_coordination_stage thread is used for firing these events. The listeners of these events can perform
 * bad operations which can bring down the client's stage thread or get it stuck. So, client need to fire cluster events
 * away from its core context for safety reasons.
 */

public class ClusterEventsBadClientsTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return ClusterEventsBadClientsTestApp.class;
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(45);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

}
