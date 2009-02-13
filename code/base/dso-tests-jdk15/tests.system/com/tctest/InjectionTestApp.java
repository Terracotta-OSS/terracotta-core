/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoNode;
import com.tc.cluster.exceptions.ClusteredListenerException;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.exception.ImplementMe;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class InjectionTestApp extends DedicatedMethodsTestApp {

  public InjectionTestApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
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

  public static class ClassWithAnnotatedInjectedInstanceNotEmpty {
    @InjectedDsoInstance
    private final DsoCluster cluster = new MockDsoCluster();

    public void checkCluster() {
      Assert.assertNotNull(cluster);
      Assert.assertTrue(cluster instanceof MockDsoCluster);
    }

    private static class MockDsoCluster implements DsoCluster {

      public void addClusterListener(final DsoClusterListener listener) throws ClusteredListenerException {
        throw new ImplementMe();
      }

      public boolean areOperationsEnabled() {
        throw new ImplementMe();
      }

      public DsoClusterTopology getClusterTopology() {
        throw new ImplementMe();
      }

      public DsoNode getCurrentNode() {
        throw new ImplementMe();
      }

      public <K> Set<K> getKeysForLocalValues(final Map<K, ?> map) throws UnclusteredObjectException {
        throw new ImplementMe();
      }

      public <K> Set<K> getKeysForOrphanedValues(final Map<K, ?> map) throws UnclusteredObjectException {
        throw new ImplementMe();
      }

      public Set<DsoNode> getNodesWithObject(final Object object) throws UnclusteredObjectException {
        throw new ImplementMe();
      }

      public Map<?, Set<DsoNode>> getNodesWithObjects(final Object... objects) throws UnclusteredObjectException {
        throw new ImplementMe();
      }

      public Map<?, Set<DsoNode>> getNodesWithObjects(final Collection<?> objects) throws UnclusteredObjectException {
        throw new ImplementMe();
      }

      public boolean isNodeJoined() {
        throw new ImplementMe();
      }

      public void removeClusterListener(final DsoClusterListener listener) {
        throw new ImplementMe();
      }

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
