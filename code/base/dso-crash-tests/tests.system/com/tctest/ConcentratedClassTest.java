/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.terracottatech.config.PersistenceMode;

/**
 * This test makes heavy use of the same TCClass stuff underneath a particular class within a single VM. I'm hoping this // *
 * test will prove to me that we have a race condition in GenricTCField.[set/get](). If we do, I'll fix it. And then
 * this test will mostly just be a regression test
 */
public class ConcentratedClassTest extends TransparentTestBase {

  public static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);

    TestTVSConfigurationSetupManagerFactory factory = (TestTVSConfigurationSetupManagerFactory) t.getConfigFactory();

    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionEnabled()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().garbageCollectionVerbose()).setValue(true);
    ((SettableConfigItem) factory.l2DSOConfig().persistenceMode()).setValue(PersistenceMode.TEMPORARY_SWAP_ONLY);

    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ConcentratedClassTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.DISK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.PERMANENT_STORE);
    setupManager.setMaxCrashCount(2);
  }

}