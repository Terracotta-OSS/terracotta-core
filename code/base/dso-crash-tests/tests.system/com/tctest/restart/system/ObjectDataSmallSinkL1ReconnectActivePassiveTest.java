/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.activepassive.ActivePassiveCrashMode;
import com.tc.test.activepassive.ActivePassivePersistenceMode;
import com.tc.test.activepassive.ActivePassiveSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;

public class ObjectDataSmallSinkL1ReconnectActivePassiveTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 6;
  private String smallSink = "250";

  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(2);
    t.initializeTestRunner();
  }
  
  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    System.setProperty("com.tc." + TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY, smallSink);
    TCPropertiesImpl.setProperty(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY, smallSink);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY + "="+smallSink);

    System.setProperty("com.tc." + TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY, smallSink);
    TCPropertiesImpl.setProperty(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY, smallSink);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY + "="+smallSink);
  }


  protected boolean enableL1Reconnect() {
    return true;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ActivePassiveCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(60);
    setupManager.setServerShareDataMode(ActivePassiveSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(ActivePassivePersistenceMode.PERMANENT_STORE);
  }

}
