/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;

public class ObjectDataActivePassiveL1ReconnectTest extends ActivePassiveTransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  protected Class<ObjectDataTestApp> getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected boolean canRunL1ProxyConnect() {
    return true;
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }
}
