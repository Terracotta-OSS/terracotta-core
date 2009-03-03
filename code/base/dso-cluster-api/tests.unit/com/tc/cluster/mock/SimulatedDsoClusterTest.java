/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.mock;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoNode;
import com.tc.cluster.simulation.SimulatedDsoCluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SimulatedDsoClusterTest extends TestCase {
  DsoCluster cluster;

  @Override
  public void setUp() {
    cluster = new SimulatedDsoCluster();
  }

  public void testCurrentNode() {
    Assert.assertNotNull(cluster.getCurrentNode());
    Assert.assertNotNull(cluster.getCurrentNode().getId());
    Assert.assertNotNull(cluster.getCurrentNode().getIp());
    Assert.assertNotNull(cluster.getCurrentNode().getHostname());
  }

  public void testListeners() {
    MockListener listener = new MockListener();
    cluster.addClusterListener(listener);
    cluster.removeClusterListener(listener);

    Assert.assertTrue(listener.isJoined());
    Assert.assertTrue(listener.isEnabled());
  }

  public void testStates() {
    Assert.assertTrue(cluster.isNodeJoined());
    Assert.assertTrue(cluster.areOperationsEnabled());
  }

  public void testTopology() {
    DsoClusterTopology topology = cluster.getClusterTopology();
    Assert.assertNotNull(topology);
    Collection<DsoNode> nodes = topology.getNodes();
    assertCollectionWithMockNodeOnly(nodes);
  }

  public void testMetaDataNullArguments() {
    Assert.assertTrue(cluster.getNodesWithObject(null).isEmpty());
    Assert.assertTrue(cluster.getNodesWithObjects((Object[])null).isEmpty());
    Assert.assertTrue(cluster.getNodesWithObjects((Collection)null).isEmpty());
    Assert.assertTrue(cluster.getKeysForOrphanedValues(null).isEmpty());
    Assert.assertTrue(cluster.getKeysForLocalValues(null).isEmpty());
  }

  public void testMetaDataGetNodesWithObject() {
    assertCollectionWithMockNodeOnly(cluster.getNodesWithObject(this));
  }

  public void testMetaDataGetNodesWithObjectsVarArgs() {
    assertMapWithMockNodeOnly(this, cluster.getNodesWithObjects(this));
  }

  public void testMetaDataGetNodesWithObjects() {
    assertMapWithMockNodeOnly(this, cluster.getNodesWithObjects(Arrays.asList(this)));
  }

  public void testMetaDataGetKeysForLocalValues() {
    Map<String, Object> testMap = new HashMap<String, Object>();
    testMap.put("key1", new Object());
    testMap.put("key2", new Object());

    final Set<String> keysForLocalValues = cluster.getKeysForLocalValues(testMap);
    Assert.assertEquals(2, keysForLocalValues.size());
    Assert.assertTrue(keysForLocalValues.contains("key1"));
    Assert.assertTrue(keysForLocalValues.contains("key2"));
  }

  public void testMetaDataGetKeysForOrphanedValues() {
    Map<String, Object> testMap = new HashMap<String, Object>();
    testMap.put("key1", new Object());
    testMap.put("key2", new Object());

    Assert.assertTrue(cluster.getKeysForOrphanedValues(testMap).isEmpty());
  }

  private void assertMapWithMockNodeOnly(final Object origObject, final Map<?, Set<DsoNode>> nodesMap) {
    Assert.assertNotNull(nodesMap);
    Assert.assertEquals(1, nodesMap.size());
    assertCollectionWithMockNodeOnly(nodesMap.get(origObject));
  }

  private void assertCollectionWithMockNodeOnly(final Collection<DsoNode> nodes) {
    Assert.assertNotNull(nodes);
    Assert.assertEquals(1, nodes.size());
    Assert.assertSame(cluster.getCurrentNode(), nodes.iterator().next());
  }

  private static class MockListener implements DsoClusterListener {

    private boolean joined = false;
    private boolean enabled = false;

    public boolean isJoined() {
      return joined;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void nodeJoined(final DsoClusterEvent event) {
      joined = true;
    }

    public void operationsEnabled(final DsoClusterEvent event) {
      enabled = true;
    }

    public void operationsDisabled(final DsoClusterEvent event) {
      Assert.fail("this event should not be triggered");
    }

    public void nodeLeft(final DsoClusterEvent event) {
      Assert.fail("this event should not be triggered");
    }
  }
}