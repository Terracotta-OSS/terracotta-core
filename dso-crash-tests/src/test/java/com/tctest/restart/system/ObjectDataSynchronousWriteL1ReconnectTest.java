/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.runtime.Os;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;

import java.util.HashMap;
import java.util.Map;

public class ObjectDataSynchronousWriteL1ReconnectTest extends ActivePassiveTransparentTestBase implements
    TestConfigurator {

  private final int clientCount = 2;

  @Override
  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  @Override
  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(ObjectDataTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected long getRestartInterval(RestartTestHelper helper) {
    if (Os.isSolaris()) {
      return super.getRestartInterval(helper) * 3;
    } else {
      return super.getRestartInterval(helper);
    }
  }

  @Override
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    // leaving this as a disk-based active-passive test just so we have one
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.DISK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

}
