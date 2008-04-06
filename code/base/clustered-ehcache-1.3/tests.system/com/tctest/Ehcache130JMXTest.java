/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.TIMUtil;

import java.util.Date;

public class Ehcache130JMXTest extends EhcacheJMXTestBase {

  public Ehcache130JMXTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends BaseApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.addModule(TIMUtil.EHCACHE_1_3, TIMUtil.getVersion(TIMUtil.EHCACHE_1_3));
    }
  }

}
