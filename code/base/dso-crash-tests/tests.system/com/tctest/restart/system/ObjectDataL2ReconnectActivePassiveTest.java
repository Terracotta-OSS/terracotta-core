/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.util.runtime.Os;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;

import java.util.HashMap;
import java.util.Map;

public class ObjectDataL2ReconnectActivePassiveTest extends ActivePassiveTransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  public ObjectDataL2ReconnectActivePassiveTest() {
    if (Os.isWindows()) {
//      System.err.println("Disabling it for windows only for now");
//      disableAllUntil("2008-05-21");
    }
  }

  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(ObjectDataTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
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
      managers[i].setProxyDownTime(100);
    }
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);

    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
