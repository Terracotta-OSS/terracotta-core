/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.util.runtime.Os;

import java.util.ArrayList;

public class LinkedBlockingQueueL2ReconnectActivePassiveTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 4;

  public LinkedBlockingQueueL2ReconnectActivePassiveTest() {
    if (Os.isWindows()) {
      // System.err.println("Disabling it for windows only for now");
      // disableAllUntil("2008-05-15");
    }
  }

  @Override
  protected void setJvmArgsL2Reconnect(final ArrayList jvmArgs) {
    super.setJvmArgsL2Reconnect(jvmArgs);
    setL2ReconnectTimout(jvmArgs, 5000);
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  @Override
  protected boolean enableL2Reconnect() {
    return true;
  }

  @Override
  protected boolean canRunL2ProxyConnect() {
    return true;
  }

  @Override
  protected void setupL2ProxyConnectTest(ProxyConnectManager[] managers) {
    /*
     * subclass can overwrite to change the test parameters.
     */
    for (int i = 0; i < managers.length; ++i) {
      managers[i].setProxyWaitTime(20 * 1000);
      managers[i].setProxyDownTime(1000);
    }
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
