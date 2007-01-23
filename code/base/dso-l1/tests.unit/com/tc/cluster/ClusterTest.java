/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import java.util.Arrays;
import java.util.Map;

import junit.framework.TestCase;

public class ClusterTest extends TestCase {

  private Cluster           cluster;
  private TestEventListener cel;

  protected void setUp() throws Exception {
    cluster = new Cluster();
    cel = new TestEventListener();
  }

  public void testGetThisNode() {
    // 0
    assertNull(cluster.getThisNode());

    // 1
    cluster.nodeConnected("someNode");
    assertNull(cluster.getThisNode());

    // 2
    cluster.nodeDisconnected("someNode");
    assertNull(cluster.getThisNode());

    // 3
    final String thisNodeId = "xxxyyyzzz";
    cluster.thisNodeConnected(thisNodeId, new String[] { "someNode" });
    assertNotNull(cluster.getThisNode());
    assertEquals(thisNodeId, cluster.getThisNode().getNodeId());
  }

  public void testGetNodes() {
    // 0
    Map nodes = cluster.getNodes();
    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());

    // 1
    final String thisNodeId = "1";
    cluster.thisNodeConnected(thisNodeId, new String[] { thisNodeId });
    nodes = cluster.getNodes();
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertNotNull(nodes.get(thisNodeId));
  }

  public void testThisNodeConnected() {
    final String thisNodeId = "1";
    final String[] nodeIds = new String[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);

    // if this node is connected, adding a cel should result in an immediate callback
    cluster.addClusterEventListener(cel);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId, cel.getLastString());
    assertTrue(Arrays.equals(nodeIds, cel.getLastStringArray()));

    // no callback if cel is added again
    cel.reset();
    cluster.addClusterEventListener(cel);
    assertNull(cel.getLastMethodCalled());
    assertNull(cel.getLastString());
    assertNull(cel.getLastStringArray());
  }

  public void testThisNodeDisconnected() {
    cluster.addClusterEventListener(cel);
    cluster.thisNodeDisconnected();
    // no callbacks should be made if thisNode has not been connected yet
    assertNull(cel.getLastMethodCalled());

    final String thisNodeId = "1";
    final String[] nodeIds = new String[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId, cel.getLastString());
    assertTrue(Arrays.equals(nodeIds, cel.getLastStringArray()));

    cluster.thisNodeDisconnected();
    assertEquals("thisNodeDisconnected", cel.getLastMethodCalled());
  }

  public void testNodeConnected() {
    // prime cluster
    final String thisNodeId = "1";
    final String[] nodeIds = new String[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);
    cluster.addClusterEventListener(cel);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId, cel.getLastString());
    assertTrue(Arrays.equals(nodeIds, cel.getLastStringArray()));
    cel.reset();

    // now cause nodeConnected event
    final String newNodeId = "2";
    cluster.nodeConnected(newNodeId);
    assertEquals("nodeConnected", cel.getLastMethodCalled());
    assertEquals(newNodeId, cel.getLastString());
    assertNull(cel.getLastStringArray());
    cel.reset();

    // now cause node disconnected event
    cluster.nodeDisconnected(newNodeId);
    assertEquals("nodeDisconnected", cel.getLastMethodCalled());
    assertEquals(newNodeId, cel.getLastString());
    assertNull(cel.getLastStringArray());
    cel.reset();

  }

  public void testNodeDisconnected() {
    // no callbacks should be made if this node has not been connected
    cluster.addClusterEventListener(cel);
    final String nodeId = "1";
    cluster.nodeDisconnected(nodeId);
    assertNull(cel.getLastMethodCalled());
    assertNull(cel.getLastStringArray());
    assertNull(cel.getLastString());
  }

  public void testClientExceptionSafety() {
    cluster.addClusterEventListener(new TestEventListenerWithExceptions());
    final String thisNodeId = "1";
    final String[] nodesCurrentlyInCluster = new String[] {thisNodeId};
    cluster.thisNodeConnected(thisNodeId , nodesCurrentlyInCluster );
    cluster.thisNodeDisconnected();
    cluster.nodeConnected(thisNodeId);
    cluster.nodeDisconnected(thisNodeId);
  }
}

class TestEventListener implements ClusterEventListener {

  private String   lastString       = null;
  private String[] lastStringArray  = null;
  private String   lastMethodCalled = null;

  public void nodeConnected(String nodeId) {
    lastString = nodeId;
    lastMethodCalled = "nodeConnected";
  }

  public void nodeDisconnected(String nodeId) {
    lastString = nodeId;
    lastMethodCalled = "nodeDisconnected";
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    lastString = thisNodeId;
    lastStringArray = nodesCurrentlyInCluster;
    lastMethodCalled = "thisNodeConnected";
  }

  public void thisNodeDisconnected(String thisNodeId) {
    lastString = thisNodeId;
    lastMethodCalled = "thisNodeDisconnected";
  }

  public String getLastMethodCalled() {
    return lastMethodCalled;
  }

  public String getLastString() {
    return lastString;
  }

  public String[] getLastStringArray() {
    return lastStringArray;
  }

  public void reset() {
    lastString = null;
    lastStringArray = null;
    lastMethodCalled = null;
  }

}

class TestEventListenerWithExceptions implements ClusterEventListener {

  public void nodeConnected(String nodeId) {
    throw new RuntimeException("nodeConnected");
  }

  public void nodeDisconnected(String nodeId) {
    throw new RuntimeException("nodeDisconnected");
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    throw new RuntimeException("thisNodeConnected");
  }

  public void thisNodeDisconnected(String thisNodeId) {
    throw new RuntimeException("thisNodeDisconnected");
  }
}
