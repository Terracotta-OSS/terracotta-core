/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.properties.TCProperties;
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

  private int    clientCount    = 6;
  private String smallL2Sink    = "50";
  private String smallSendQueue = "50";

  public ObjectDataSmallSinkL1ReconnectActivePassiveTest() {
    // MNK-568
    // disableAllUntil("2008-07-15");
  }

  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(2);
    t.initializeTestRunner();
  }

  protected void setExtraJvmArgs(final ArrayList jvmArgs) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    
    System.setProperty("com.tc." + TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY, smallL2Sink);
    tcProps.setProperty(TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY, smallL2Sink);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_SEDA_STAGE_SINK_CAPACITY + "="+smallL2Sink);

    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP, smallSendQueue);
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP, smallSendQueue);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_SENDQUEUE_CAP + "=" + smallSendQueue);
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
