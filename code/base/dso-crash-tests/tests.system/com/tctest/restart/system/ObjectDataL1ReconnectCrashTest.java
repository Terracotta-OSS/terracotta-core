/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ObjectDataL1ReconnectCrashTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }
  
  protected boolean enableL1Reconnect() {
    return true;
  }
}