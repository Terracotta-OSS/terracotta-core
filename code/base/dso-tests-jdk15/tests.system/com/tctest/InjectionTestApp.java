/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import junit.framework.Assert;

public class InjectionTestApp extends AbstractTransparentApp {

  public InjectionTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public void run() {
    testInjection();
    testInjectionDefaultConstructor();
  }

  private void testInjection() {
    new ClassWithAnnotatedInjectedInstance();
  }

  private void testInjectionDefaultConstructor() {
    (new ClassWithAnnotatedInjectedInstanceDefaultConstructor()).checkCluster();
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.addIncludePattern(ClassWithAnnotatedInjectedInstance.class.getName());
    config.addIncludePattern(ClassWithAnnotatedInjectedInstanceDefaultConstructor.class.getName());
  }

  public static class ClassWithAnnotatedInjectedInstance {
    @InjectedDsoInstance
    private DsoCluster cluster;

    public ClassWithAnnotatedInjectedInstance() {
      Assert.assertNotNull(cluster);
    }
  }

  public static class ClassWithAnnotatedInjectedInstanceDefaultConstructor {
    @InjectedDsoInstance
    private DsoCluster cluster;

    public void checkCluster() {
      Assert.assertNotNull(cluster);
    }
  }
}
