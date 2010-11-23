/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.terracottatech.config.PersistenceMode;

/**
 * This test makes heavy use of the same TCClass stuff underneath a particular class within a single VM. I'm hoping this // *
 * test will prove to me that we have a race condition in GenricTCField.[set/get](). If we do, I'll fix it. And then
 * this test will mostly just be a regression test
 */
public class ConcentratedClassTest extends ActivePassiveTransparentTestBase {

  public static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    configFactory.setGCEnabled(true);
    configFactory.setGCVerbose(true);
    configFactory.setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  protected Class getApplicationClass() {
    return ConcentratedClassTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
    setupManager.setMaxCrashCount(2);
  }

}