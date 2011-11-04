/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;

public class ConcurrentHashMapGCActivePassiveTest extends GCAndActivePassiveTestBase implements TestConfigurator {

  public ConcurrentHashMapGCActivePassiveTest() {
    // Disabled for MNK-2545
    timebombTest("2013-01-15");
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(ConcurrentHashMapSwappingTestApp.GC_TEST_KEY, "true");
    super.doSetUp(t);
  }

  @Override
  protected Class getApplicationClass() {
    return ConcurrentHashMapSwappingTestApp.class;
  }

  @Override
  public void setupActivePassiveTest(final ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(20);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
