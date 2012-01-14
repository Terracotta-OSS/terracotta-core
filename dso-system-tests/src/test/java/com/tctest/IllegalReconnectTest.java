/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.proxy.ProxyConnectManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import com.tctest.runner.TransparentAppConfig;

public class IllegalReconnectTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void tearDown() throws Exception {
    getProxyConnectManager().close();
    super.tearDown();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  @Override
  protected boolean canRunL1ProxyConnect() {
    return true;
  }

  @Override
  protected boolean enableL1Reconnect() {
    return false;
  }

  @Override
  protected boolean canSkipL1ReconnectCheck() {
    return true;
  }

  @Override
  protected boolean enableManualProxyConnectControl() {
    return true;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final ProxyConnectManager proxyMgr;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      assertEquals(1, cfg.getGlobalParticipantCount());
      this.proxyMgr = (ProxyConnectManager) cfg.getAttributeObject(TransparentAppConfig.PROXY_CONNECT_MGR);
    }

    @Override
    protected void runTest() throws Throwable {
      proxyMgr.closeClientConnections();
      ThreadUtil.reallySleep(10000);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      //
    }

  }

}
