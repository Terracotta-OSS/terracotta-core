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

public class InjectionOnLoadWithScriptOnLoadTestApp extends DedicatedMethodsTestApp {
  public final static String ONLOAD_STRING = "onload script was called";

  private final ClassWithAnnotatedInjectedInstances rootInjection = new ClassWithAnnotatedInjectedInstances();

  public InjectionOnLoadWithScriptOnLoadTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  void testInjection() {
    Assert.assertNotNull(rootInjection.getCluster1());
    Assert.assertNotNull(rootInjection.getCluster2());
    Assert.assertEquals(ONLOAD_STRING, rootInjection.getOnLoadData());
  }

  public static class ClassWithAnnotatedInjectedInstances {
    @InjectedDsoInstance
    private DsoCluster cluster1;

    @InjectedDsoInstance
    private DsoCluster cluster2;

    private transient String onLoadData;

    public ClassWithAnnotatedInjectedInstances() {
      Assert.assertNotNull(cluster1);
      Assert.assertNotNull(cluster2);
      onLoadData = ONLOAD_STRING;
    }

    public String getOnLoadData() {
      return onLoadData;
    }

    public DsoCluster getCluster1() {
      return cluster1;
    }

    public DsoCluster getCluster2() {
      return cluster2;
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = InjectionOnLoadWithScriptOnLoadTestApp.class.getName();
    TransparencyClassSpec specTestClass = config.getOrCreateSpec(testClass);
    specTestClass.addRoot("rootInjection", "rootInjection");

    String rootInjectionClass = ClassWithAnnotatedInjectedInstances.class.getName();
    TransparencyClassSpec specRootInjectionClass = config.getOrCreateSpec(rootInjectionClass);
    specRootInjectionClass.addRoot("rootInjection", "rootInjection");
    specRootInjectionClass.setHonorTransient(true);
    specRootInjectionClass.setExecuteScriptOnLoad("self.onLoadData = \"" + ONLOAD_STRING + "\"");
  }

  @Override
  protected CyclicBarrier getBarrierForNodeCoordination() {
    return null;
  }
}
