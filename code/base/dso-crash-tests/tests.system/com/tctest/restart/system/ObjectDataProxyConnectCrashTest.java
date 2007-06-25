/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
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

  protected boolean canRunProxyConnect() {
    return true;
  }
  
  protected boolean canRunCrash() {
    return true;
  }
  
  protected boolean enableL1Reconnect() {
    return true;
  }

  protected void setupProxyConnectTest(ProxyConnectManagerImpl mgr) {
    mgr.setProxyWaitTime(20 * 1000);
    mgr.setProxyDownTime(100);
  }

}