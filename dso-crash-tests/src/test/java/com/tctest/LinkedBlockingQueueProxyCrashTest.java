/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.proxyconnect.ProxyConnectManager;

public class LinkedBlockingQueueProxyCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 4;

  public LinkedBlockingQueueProxyCrashTest() {
    disableAllUntil("2011-10-01");
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
  protected boolean canRunCrash() {
    return true;
  }

  @Override
  protected boolean canRunL1ProxyConnect() {
    return true;
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  @Override
  protected void setupL1ProxyConnectTest(ProxyConnectManager mgr) {
    mgr.setProxyWaitTime(30 * 1000);
    mgr.setProxyDownTime(100);
  }

}
