/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class BundleClassExportLoadClassTestApp extends AbstractErrorCatchingTransparentApp {

  public BundleClassExportLoadClassTestApp(final String appId, final ApplicationConfig cfg,
                                  final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  protected void runTest() throws Throwable {
    // CDV-598
    getClass().getClassLoader().loadClass("org.terracotta.modules.test.DummyClass");
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    //
  }
}
