/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.DedicatedMethodsTestApp;

import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class InjectionAnnotationTestApp extends DedicatedMethodsTestApp {

  public InjectionAnnotationTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  void testInjection() {
    new ClassWithAnnotatedInjectedInstance();
  }

  void testInjectionDefaultConstructor() {
    (new ClassWithAnnotatedInjectedInstanceDefaultConstructor()).checkCluster();
  }

  void testInjectionNotEmpty() {
    (new ClassWithAnnotatedInjectedInstanceNotEmpty()).checkCluster();
  }

  public static class ClassWithAnnotatedInjectedInstance {
    @InjectedDsoInstance
    private DsoCluster cluster;

    @InjectedDsoInstance
    private DsoCluster cluster2;


    public ClassWithAnnotatedInjectedInstance() {
      Assert.assertNotNull(cluster);
      Assert.assertNotNull(cluster2);
    }
  }

  public static class ClassWithAnnotatedInjectedInstanceDefaultConstructor {
    @InjectedDsoInstance
    private DsoCluster cluster;

    public void checkCluster() {
      Assert.assertNotNull(cluster);
    }
  }

  public static class ClassWithAnnotatedInjectedInstanceNotEmpty {
    @InjectedDsoInstance
    private final DsoCluster cluster = new DummyDsoCluster();

    public void checkCluster() {
      Assert.assertNotNull(cluster);
      Assert.assertFalse(cluster instanceof DummyDsoCluster);
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.addIncludePattern(ClassWithAnnotatedInjectedInstance.class.getName());
    config.addIncludePattern(ClassWithAnnotatedInjectedInstanceDefaultConstructor.class.getName());
    config.addIncludePattern(ClassWithAnnotatedInjectedInstanceNotEmpty.class.getName());
  }

  @Override
  protected CyclicBarrier getBarrierForNodeCoordination() {
    return null;
  }
  
}
