/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.cluster.DsoCluster;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.DedicatedMethodsTestApp;

import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class InjectionConfigTestApp extends DedicatedMethodsTestApp {

  public InjectionConfigTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  void testInjection() {
    new ClassWithConfigInjectedInstance();
  }

  void testInjectionDefaultConstructor() {
    (new ClassWithConfigInjectedInstanceDefaultConstructor()).checkCluster();
  }

  void testInjectionNotEmpty() {
    (new ClassWithConfigInjectedInstanceNotEmpty()).checkCluster();
  }

  public static class ClassWithConfigInjectedInstance {
    private DsoCluster cluster;
    private DsoCluster cluster2;

    public ClassWithConfigInjectedInstance() {
      Assert.assertNotNull(cluster);
      Assert.assertNotNull(cluster2);
    }
  }

  public static class ClassWithConfigInjectedInstanceDefaultConstructor {
    private DsoCluster cluster;

    public void checkCluster() {
      Assert.assertNotNull(cluster);
    }
  }

  public static class ClassWithConfigInjectedInstanceNotEmpty {
    private final DsoCluster cluster = new DummyDsoCluster();

    public void checkCluster() {
      Assert.assertNotNull(cluster);
      Assert.assertFalse(cluster instanceof DummyDsoCluster);
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.addIncludePattern(ClassWithConfigInjectedInstance.class.getName());
    config.addIncludePattern(ClassWithConfigInjectedInstanceDefaultConstructor.class.getName());
    config.addIncludePattern(ClassWithConfigInjectedInstanceNotEmpty.class.getName());
    config.addInjectedField(ClassWithConfigInjectedInstance.class.getName(), "cluster", null);
    config.addInjectedField(ClassWithConfigInjectedInstance.class.getName(), "cluster2", null);
    config.addInjectedField(ClassWithConfigInjectedInstanceDefaultConstructor.class.getName(), "cluster", null);
    config.addInjectedField(ClassWithConfigInjectedInstanceNotEmpty.class.getName(), "cluster", null);
  }

  @Override
  protected CyclicBarrier getBarrierForNodeCoordination() {
    return null;
  }
}
