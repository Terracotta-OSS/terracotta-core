/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.runtime.Os;
import com.tctest.Memory;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ObjectDataProxyConnectCrashTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  public ObjectDataProxyConnectCrashTest() {
    //
  }


  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunL1ProxyConnect() {
    return true;
  }
  
  @Override
  protected long getRestartInterval(RestartTestHelper helper) {
    if(Os.isSolaris() || Memory.isMemoryLow()) {
      return super.getRestartInterval(helper) * 3;
    } else {
      return super.getRestartInterval(helper);
    }
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean enableL1Reconnect() {
    return true;
  }

  protected void setupL1ProxyConnectTest(ProxyConnectManager mgr) {
    mgr.setProxyWaitTime(30 * 1000);
    mgr.setProxyDownTime(100);
  }

}
