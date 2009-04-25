/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.exception.TCRuntimeException;
import com.tc.test.proxyconnect.ProxyConnectManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class LinkedBlockingQueueProxyCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 4;

  public LinkedBlockingQueueProxyCrashTest() {
    String computerName;
    try {
      computerName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new TCRuntimeException(e);
    }
    if (computerName.startsWith("rh5mo0")) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean canRunL1ProxyConnect() {
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
