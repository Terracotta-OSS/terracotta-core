/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tcclient.cluster.DsoNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ClusterMetaDataTestApp extends DedicatedMethodsTestApp {

  private final CyclicBarrier barrier = new CyclicBarrier(ClusterMetaDataTest.NODE_COUNT);

  private final SomePojo      pojo    = new SomePojo();
  private final Map           map     = new HashMap();
  private final Map           treeMap = new TreeMap();

  @InjectedDsoInstance
  private DsoCluster          cluster;

  public ClusterMetaDataTestApp(final String appId, final ApplicationConfig config,
                                final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  void testNodeMetaData() {
    Assert.assertNotNull(cluster.getCurrentNode().getIp());
    Assert.assertNotNull(cluster.getCurrentNode().getHostname());
    Assert.assertEquals("127.0.0.1", cluster.getCurrentNode().getIp());
    Assert.assertNotNull(cluster.getCurrentNode().getHostname());
  }

  void testGetNodesWithObjectUnclustered() {
    final Object unclustered = new Object();
    try {
      cluster.getNodesWithObject(unclustered);
      Assert.fail("Expected exception");
    } catch (UnclusteredObjectException e) {
      Assert.assertSame(unclustered, e.getUnclusteredObject());
    }
  }

  void testGetNodesWithObjectNull() {
    final Set<DsoNode> result = cluster.getNodesWithObject(null);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.size());
  }

  void testGetNodesWithObjectsNull() {
    final Map<?, Set<DsoNode>> result1 = cluster.getNodesWithObjects((Object[]) null);
    Assert.assertNotNull(result1);
    Assert.assertEquals(0, result1.size());

    final Map<?, Set<DsoNode>> result2 = cluster.getNodesWithObjects((Collection) null);
    Assert.assertNotNull(result2);
    Assert.assertEquals(0, result2.size());
  }

  void testGetNodesWithObjectsNullElement() {
    final Map<?, Set<DsoNode>> result1 = cluster.getNodesWithObjects((Object) null);
    Assert.assertNotNull(result1);
    Assert.assertEquals(0, result1.size());

    final Map<?, Set<DsoNode>> result2 = cluster.getNodesWithObjects(Arrays.asList((Object) null));
    Assert.assertNotNull(result2);
    Assert.assertEquals(0, result2.size());
  }

  void testGetNodesWithObjectsUnclustered() {
    final Object unclustered = new Object();
    try {
      cluster.getNodesWithObjects(unclustered);
      Assert.fail("Expected exception");
    } catch (UnclusteredObjectException e) {
      Assert.assertSame(unclustered, e.getUnclusteredObject());
    }

    try {
      cluster.getNodesWithObjects(pojo, map, unclustered);
      Assert.fail("Expected exception");
    } catch (UnclusteredObjectException e) {
      Assert.assertSame(unclustered, e.getUnclusteredObject());
    }

    try {
      cluster.getNodesWithObjects(Arrays.asList(pojo, map, unclustered));
      Assert.fail("Expected exception");
    } catch (UnclusteredObjectException e) {
      Assert.assertSame(unclustered, e.getUnclusteredObject());
    }
  }

  void testGetNodesWithObject() throws InterruptedException, BrokenBarrierException {
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

    if (0 == barrier.await()) {
      pojo.setYourMojo(null);
    }
  }

  void testGetNodesWithObjectMap() throws InterruptedException, BrokenBarrierException {
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

    if (0 == barrier.await()) {
      synchronized (map) {
        map.clear();
      }
    }
  }

  void testGetNodesWithObjects() throws InterruptedException, BrokenBarrierException {
    final int nodeId = barrier.await();

    if (1 == nodeId) {
      pojo.setYourMojo(new YourMojo("I love your mojo"));
    }

    if (2 == nodeId) {
      pojo.setMyMojo(new MyMojo("I give you my mojo"));
    }

    barrier.await();

    final DsoNode currentNode = cluster.getCurrentNode();

    if (1 == nodeId) {
      final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(pojo.getYourMojo());
      Assert.assertEquals(1, nodes.size());
      Assert.assertTrue(nodes.get(pojo.getYourMojo()).contains(currentNode));
      Assert.assertEquals(1, nodes.get(pojo.getYourMojo()).size());
    }

    if (2 == nodeId) {
      final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(pojo.getMyMojo());
      Assert.assertEquals(1, nodes.size());
      Assert.assertTrue(nodes.get(pojo.getMyMojo()).contains(currentNode));
      Assert.assertEquals(1, nodes.get(pojo.getMyMojo()).size());
    }

    barrier.await();

    if (0 == nodeId) {
      checkGetNodesWithObjectsResult(cluster.getNodesWithObjects(pojo.getYourMojo(), null, pojo.getMyMojo()));
      checkGetNodesWithObjectsResult(cluster.getNodesWithObjects(Arrays.asList(pojo.getYourMojo(), null, pojo
          .getMyMojo())));
    }

    barrier.await();

    if (1 == nodeId) {
      final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(pojo.getYourMojo());
      Assert.assertEquals(1, nodes.size());
      Assert.assertEquals(2, nodes.get(pojo.getYourMojo()).size());
    }

    if (2 == nodeId) {
      final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(pojo.getMyMojo());
      Assert.assertEquals(1, nodes.size());
      Assert.assertEquals(2, nodes.get(pojo.getMyMojo()).size());
    }

    if (0 == barrier.await()) {
      pojo.setYourMojo(null);
    }
  }

  private void checkGetNodesWithObjectsResult(final Map<?, Set<DsoNode>> nodes) {
    final DsoNode currentNode = cluster.getCurrentNode();

    Assert.assertEquals(2, nodes.size());

    final Set<DsoNode> yourMojoNodeSet = nodes.get(pojo.getYourMojo());
    Assert.assertTrue(yourMojoNodeSet.contains(currentNode));
    Assert.assertEquals(2, yourMojoNodeSet.size());
    DsoNode otherNodeWithYourMojo = null;
    for (DsoNode node : yourMojoNodeSet) {
      if (!node.equals(currentNode)) {
        otherNodeWithYourMojo = node;
        break;
      }
    }

    Assert.assertNotNull(otherNodeWithYourMojo);

    final Set<DsoNode> myMojoNodeSet = nodes.get(pojo.getMyMojo());
    Assert.assertTrue(myMojoNodeSet.contains(currentNode));
    Assert.assertEquals(2, myMojoNodeSet.size());
    DsoNode otherNodeWithMyMojo = null;
    for (DsoNode node : myMojoNodeSet) {
      if (!node.equals(currentNode)) {
        otherNodeWithMyMojo = node;
        break;
      }
    }

    Assert.assertNotNull(otherNodeWithMyMojo);

    Assert.assertFalse(yourMojoNodeSet.contains(otherNodeWithMyMojo));
    Assert.assertFalse(myMojoNodeSet.contains(otherNodeWithYourMojo));
  }

  void testGetNodesWithObjectsMap() throws InterruptedException, BrokenBarrierException {
    final int nodeId = barrier.await();

    if (1 == nodeId) {
      synchronized (map) {
        map.put("key1", new SomePojo());
      }
    }

    if (2 == nodeId) {
      synchronized (map) {
        map.put("key2", new SomePojo());
      }
    }

    barrier.await();

    final DsoNode currentNode = cluster.getCurrentNode();

    if (1 == nodeId) {
      synchronized (map) {
        final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(map.get("key1"));
        Assert.assertEquals(1, nodes.size());
        Assert.assertTrue(nodes.get(map.get("key1")).contains(currentNode));
        Assert.assertEquals(1, nodes.get(map.get("key1")).size());
      }
    }

    if (2 == nodeId) {
      synchronized (map) {
        final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(map.get("key2"));
        Assert.assertEquals(1, nodes.size());
        Assert.assertTrue(nodes.get(map.get("key2")).contains(currentNode));
        Assert.assertEquals(1, nodes.get(map.get("key2")).size());
      }
    }

    barrier.await();

    if (0 == nodeId) {
      checkGetNodesWithObjectsMapResult(cluster.getNodesWithObjects(map.values()));
      checkGetNodesWithObjectsMapResult(cluster.getNodesWithObjects(map.get("key1"), map.get("key2")));
    }

    barrier.await();

    if (1 == nodeId) {
      synchronized (map) {
        final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(map.get("key1"));
        Assert.assertEquals(2, nodes.get(map.get("key1")).size());
      }
    }

    if (2 == nodeId) {
      synchronized (map) {
        final Map<?, Set<DsoNode>> nodes = cluster.getNodesWithObjects(map.get("key2"));
        Assert.assertEquals(2, nodes.get(map.get("key2")).size());
      }
    }

    if (0 == barrier.await()) {
      synchronized (map) {
        map.clear();
      }
    }
  }

  private void checkGetNodesWithObjectsMapResult(final Map<?, Set<DsoNode>> nodes) {
    final DsoNode currentNode = cluster.getCurrentNode();

    Assert.assertEquals(2, nodes.size());

    final Set<DsoNode> key1NodeSet = nodes.get(map.get("key1"));
    Assert.assertTrue(key1NodeSet.contains(currentNode));
    Assert.assertEquals(2, key1NodeSet.size());
    DsoNode otherNodeWithKey1Value = null;
    for (DsoNode node : key1NodeSet) {
      if (!node.equals(currentNode)) {
        otherNodeWithKey1Value = node;
        break;
      }
    }

    Assert.assertNotNull(otherNodeWithKey1Value);

    final Set<DsoNode> key2NodeSet = nodes.get(map.get("key2"));
    Assert.assertTrue(key2NodeSet.contains(currentNode));
    Assert.assertEquals(2, key2NodeSet.size());
    DsoNode otherNodeWithKey2Value = null;
    for (DsoNode node : key2NodeSet) {
      if (!node.equals(currentNode)) {
        otherNodeWithKey2Value = node;
        break;
      }
    }

    Assert.assertNotNull(otherNodeWithKey2Value);

    Assert.assertFalse(key1NodeSet.contains(otherNodeWithKey2Value));
    Assert.assertFalse(key2NodeSet.contains(otherNodeWithKey1Value));
  }

  void testGetKeysForLocalValuesUnclustered() {
    final Map unclustered = new HashMap();
    try {
      cluster.getKeysForLocalValues(unclustered);
      Assert.fail("Expected exception");
    } catch (UnclusteredObjectException e) {
      Assert.assertSame(unclustered, e.getUnclusteredObject());
    }
  }

  void testGetKeysForLocalValuesNull() {
    final Set result = cluster.getKeysForLocalValues(null);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.size());
  }

  void testGetKeysForLocalValuesNotPartial() {
    final Set result = cluster.getKeysForLocalValues(treeMap);
    Assert.assertNotNull(result);
    Assert.assertEquals(0, result.size());
  }

  void testGetKeysForLocalValues() throws InterruptedException, BrokenBarrierException {
    final int nodeId = barrier.await();

    if (1 == nodeId) {
      synchronized (map) {
        map.put("key1", new SomePojo());
      }
    }

    if (2 == nodeId) {
      synchronized (map) {
        map.put("key2", new SomePojo());
      }
    }

    barrier.await();

    if (0 == nodeId) {
      synchronized (map) {
        map.get("key1");
        map.get("key2");
      }
    }

    barrier.await();

    if (1 == nodeId) {
      final Set<String> localKeys = cluster.getKeysForLocalValues(map);
      Assert.assertNotNull(localKeys);
      Assert.assertEquals(1, localKeys.size());
      Assert.assertTrue(localKeys.contains("key1"));
    }

    if (2 == nodeId) {
      final Set<String> localKeys = cluster.getKeysForLocalValues(map);
      Assert.assertNotNull(localKeys);
      Assert.assertEquals(1, localKeys.size());
      Assert.assertTrue(localKeys.contains("key2"));
    }

    if (0 == nodeId) {
      final Set<String> localKeys = cluster.getKeysForLocalValues(map);
      Assert.assertNotNull(localKeys);
      Assert.assertEquals(2, localKeys.size());
      Assert.assertTrue(localKeys.contains("key1"));
      Assert.assertTrue(localKeys.contains("key2"));
    }

    if (0 == barrier.await()) {
      synchronized (map) {
        map.clear();
      }
    }
  }

  @Override
  protected CyclicBarrier getBarrierForNodeCoordination() {
    return barrier;
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterMetaDataTestApp.class.getName();
    TransparencyClassSpec specTestClass = config.getOrCreateSpec(testClass);
    specTestClass.addRoot("pojo", "pojo");
    specTestClass.addRoot("map", "map");
    specTestClass.addRoot("treeMap", "treeMap");
    specTestClass.addRoot("barrier", "barrier");

    config.addWriteAutolock("* " + testClass + "*.*(..)");
    config.addIncludePattern(testClass + "$*");

    config.addWriteAutolock("* " + SomePojo.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + AbstractMojo.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + YourMojo.class.getName() + "*.*(..)");

    config.addWriteAutolock("* " + MyMojo.class.getName() + "*.*(..)");
  }

  public static class SomePojo {
    private YourMojo yourMojo;
    private MyMojo   myMojo;

    public SomePojo() {
      // default constructor
    }

    public SomePojo(final YourMojo yourMojo, final MyMojo myMojo) {
      this.yourMojo = yourMojo;
      this.myMojo = myMojo;
    }

    public synchronized YourMojo getYourMojo() {
      return yourMojo;
    }

    public synchronized void setYourMojo(final YourMojo yourMojo) {
      this.yourMojo = yourMojo;
    }

    public synchronized MyMojo getMyMojo() {
      return myMojo;
    }

    public synchronized void setMyMojo(final MyMojo myMojo) {
      this.myMojo = myMojo;
    }
  }

  public static abstract class AbstractMojo {

    protected String mojo;

    public synchronized String getMojo() {
      return mojo;
    }

    public synchronized void setMojo(final String mojo) {
      this.mojo = mojo;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((mojo == null) ? 0 : mojo.hashCode());
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MyMojo other = (MyMojo) obj;
      if (mojo == null) {
        if (other.mojo != null) return false;
      } else if (!mojo.equals(other.mojo)) return false;
      return true;
    }

  }

  public static class YourMojo extends AbstractMojo {
    public YourMojo(final String mojo) {
      this.mojo = mojo;
    }
  }

  public static class MyMojo extends AbstractMojo {
    public MyMojo(final String mojo) {
      this.mojo = mojo;
    }
  }
}
