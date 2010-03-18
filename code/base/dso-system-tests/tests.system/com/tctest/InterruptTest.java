/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.util.ArrayList;
import java.util.Date;

public class InterruptTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;
  private boolean          reconnectOriginalValue;

  public InterruptTest() {
    // MNK-565
    if (Os.isSolaris() && !Vm.isJDK16Compliant()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    reconnectOriginalValue = tcProps.getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED);
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "false");
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "false");
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED + "=false");
  }

  protected Class getApplicationClass() {
    return InterruptTestApp.class;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + reconnectOriginalValue);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "" + reconnectOriginalValue);
  }
}
