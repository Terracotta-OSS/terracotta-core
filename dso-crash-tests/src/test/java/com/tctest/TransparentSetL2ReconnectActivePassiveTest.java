/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.proxy.ProxyConnectManager;
import com.tc.util.runtime.Os;

import java.util.ArrayList;

public class TransparentSetL2ReconnectActivePassiveTest extends ActivePassiveTransparentTestBase implements
    TestConfigurator {
  private static final int NODE_COUNT           = 3;
  private static final int EXECUTION_COUNT      = 3;
  private static final int LOOP_ITERATION_COUNT = 400;

  @Override
  protected Class getApplicationClass() {
    return TransparentSetTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATION_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected boolean enableL2Reconnect() {
    return true;
  }

  @Override
  protected void setL2ReconnectTimout(final ArrayList jvmArgs, int timeoutMilliSecond) {

    if (Os.isSolaris()) {
      String timeoutString = Integer.toString(10000);
      TCProperties tcProps = TCPropertiesImpl.getProperties();
      tcProps.setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT, timeoutString);
      System.setProperty("com.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT, timeoutString);
      jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT + "=" + timeoutString);
    } else {
      super.setL2ReconnectTimout(jvmArgs, timeoutMilliSecond);
    }
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
      managers[i].setProxyDownTime(500);
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
