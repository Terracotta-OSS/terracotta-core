/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoNode;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClusterMetaDataTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier = new CyclicBarrier(ClusterMetaDataTest.NODE_COUNT);

  private final SomePojo      pojo    = new SomePojo();
  private final HashMap       map     = new HashMap();

  @InjectedDsoInstance
  private DsoCluster          cluster;

  public ClusterMetaDataTestApp(final String appId, final ApplicationConfig config,
                                final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  public void run() {
    try {
      testGetNodesWithObject();
      testGetNodesWithObjectMap();

    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void testGetNodesWithObject() throws InterruptedException, BrokenBarrierException {
    final int nodeId = barrier.await();

    if (1 == nodeId) {
      pojo.setYourMojo(new YourMojo("I love your mojo"));
    }

    barrier.await();

    final DsoNode currentNode = cluster.getCurrentNode();

    if (1 == nodeId) {
      final Set<DsoNode> nodes = cluster.getNodesWithObject(pojo.getYourMojo());
      Assert.assertTrue(nodes.contains(currentNode));
      Assert.assertEquals(1, nodes.size());
    }

    barrier.await();

    if (nodeId != 0) {
      pojo.getYourMojo();
    }

    barrier.await();

    if (nodeId != 0) {
      final Set<DsoNode> nodes = cluster.getNodesWithObject(pojo.getYourMojo());
      Assert.assertTrue(nodes.contains(currentNode));
      Assert.assertEquals(2, nodes.size());
    }

    barrier.await();

    pojo.getYourMojo();

    barrier.await();

    final Set<DsoNode> nodes = cluster.getNodesWithObject(pojo.getYourMojo());
    Assert.assertTrue(nodes.contains(currentNode));
    Assert.assertEquals(3, nodes.size());
  }

  private void testGetNodesWithObjectMap() throws InterruptedException, BrokenBarrierException {
    final int nodeId = barrier.await();

    if (1 == nodeId) {
      synchronized (map) {
        map.put("key", new SomePojo());
      }
    }

    barrier.await();

    final DsoNode currentNode = cluster.getCurrentNode();

    if (1 == nodeId) {
      synchronized (map) {
        final Set<DsoNode> nodes = cluster.getNodesWithObject(map.get("key"));
        Assert.assertTrue(nodes.contains(currentNode));
        Assert.assertEquals(1, nodes.size());
      }
    }

    barrier.await();

    if (nodeId != 0) {
      map.get("key");
    }

    barrier.await();

    if (nodeId != 0) {
      synchronized (map) {
        final Set<DsoNode> nodes = cluster.getNodesWithObject(map.get("key"));
        Assert.assertTrue(nodes.contains(currentNode));
        Assert.assertEquals(2, nodes.size());
      }
    }

    barrier.await();

    map.get("key");

    barrier.await();

    synchronized (map) {
      final Set<DsoNode> nodes = cluster.getNodesWithObject(map.get("key"));
      Assert.assertTrue(nodes.contains(currentNode));
      Assert.assertEquals(3, nodes.size());
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterMetaDataTestApp.class.getName();
    TransparencyClassSpec specTestClass = config.getOrCreateSpec(testClass);
    specTestClass.addRoot("pojo", "pojo");
    specTestClass.addRoot("map", "map");
    specTestClass.addRoot("barrier", "barrier");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    config.addIncludePattern(testClass + "$*");

    config.addWriteAutolock("* " + SomePojo.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + YourMojo.class.getName() + "*.*(..)");
  }

  public static class SomePojo {
    private YourMojo yourMojo;

    public synchronized YourMojo getYourMojo() {
      return yourMojo;
    }

    public synchronized void setYourMojo(final YourMojo yourMojo) {
      this.yourMojo = yourMojo;
    }
  }

  public static class YourMojo {
    private String mojo;

    public YourMojo(final String mojo) {
      this.mojo = mojo;
    }

    public synchronized String getMojo() {
      return mojo;
    }

    public synchronized void setMojo(final String mojo) {
      this.mojo = mojo;
    }
  }
}
