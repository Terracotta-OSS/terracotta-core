/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.util.runtime.Os;

public class LinkedBlockingQueueL2ReconnectActivePassiveTest extends TransparentTestBase {

  private static final int NODE_COUNT = 4;
  
  public LinkedBlockingQueueL2ReconnectActivePassiveTest() {
    if (Os.isWindows()) {
      System.err.println("Disabling it for windows only for now");
      disableAllUntil("2008-05-15");
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }
  
  protected boolean canRunActivePassive() {
    return true;
  }
  
  protected boolean enableL2Reconnect() {
    return true;
  }
  
  protected boolean canRunL2ProxyConnect() {
    return true;
  }

  protected void setupL2ProxyConnectTest(ProxyConnectManager[] managers) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    for (int i = 0; i < managers.length; ++i) {
      managers[i].setProxyWaitTime(20 * 1000);
      managers[i].setProxyDownTime(1000);
    }
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  }
 
}
