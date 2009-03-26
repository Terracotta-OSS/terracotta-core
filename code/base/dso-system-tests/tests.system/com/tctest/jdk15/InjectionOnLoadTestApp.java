/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.cluster.DsoCluster;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.DedicatedMethodsTestApp;

import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class InjectionOnLoadTestApp extends DedicatedMethodsTestApp {
  private final ClassWithAnnotatedInjectedInstances rootInjection = new ClassWithAnnotatedInjectedInstances();

  public InjectionOnLoadTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  void testInjection() {
    Assert.assertNotNull(rootInjection.getCluster1());
    Assert.assertNotNull(rootInjection.getCluster2());
  }

  public static class ClassWithAnnotatedInjectedInstances {
    @InjectedDsoInstance
    private DsoCluster cluster1;

    @InjectedDsoInstance
    private DsoCluster cluster2;


    public ClassWithAnnotatedInjectedInstances() {
      Assert.assertNotNull(cluster1);
      Assert.assertNotNull(cluster2);
    }

    public DsoCluster getCluster1() {
      return cluster1;
    }

    public DsoCluster getCluster2() {
      return cluster2;
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = InjectionOnLoadTestApp.class.getName();
    TransparencyClassSpec specTestClass = config.getOrCreateSpec(testClass);
    specTestClass.addRoot("rootInjection", "rootInjection");

    config.addIncludePattern(ClassWithAnnotatedInjectedInstances.class.getName());
  }

  @Override
  protected CyclicBarrier getBarrierForNodeCoordination() {
    return null;
  }
}
