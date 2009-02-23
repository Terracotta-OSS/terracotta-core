/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.test.TCTestCase;

import java.util.Arrays;
import java.util.Map;

public class ClusterTest extends TCTestCase {

  private Cluster           cluster;
  private TestEventListener cel;

  @Override
  protected void setUp() throws Exception {
    cluster = new Cluster();
    cel = new TestEventListener();
  }

  public void testGetThisNode() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    cluster.thisNodeConnected(thisNodeId, new ClientID[] { new ClientID(new ChannelID(2)) });
    assertNotNull(cluster.getThisNode());
    assertEquals(thisNodeId.toString(), cluster.getThisNode().getNodeId());
  }

  public void testGetNodes() {
    // 0
    Map nodes = cluster.getNodes();
    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());

    // 1
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    cluster.thisNodeConnected(thisNodeId, new ClientID[] { thisNodeId });
    nodes = cluster.getNodes();
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertNotNull(nodes.get(thisNodeId.toString()));
  }

  public void testThisNodeConnected() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);

    // if this node is connected, adding a cel should result in an immediate callback
    cluster.addClusterEventListener(cel);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId.toString(), cel.getLastString());
    assertTrue(Arrays.equals(new String[] { thisNodeId.toString() }, cel.getLastStringArray()));

    // should not fire multiple thisNodeConnected events in a row...
    cel.reset();
    cluster.thisNodeConnected(thisNodeId, nodeIds);
    assertNull(cel.getLastMethodCalled());

    // but it should be fired after thisNodeDisconnected...
    cluster.thisNodeDisconnected();
    cluster.thisNodeConnected(thisNodeId, nodeIds);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId.toString(), cel.getLastString());
    assertTrue(Arrays.equals(new String[] { thisNodeId.toString() }, cel.getLastStringArray()));


    // no callback if cel is added again
    cel.reset();
    cluster.addClusterEventListener(cel);
    assertNull(cel.getLastMethodCalled());
    assertNull(cel.getLastString());
    assertNull(cel.getLastStringArray());
  }

  public void testThisNodeDisconnected() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);

    cluster.addClusterEventListener(cel);

    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId.toString(), cel.getLastString());
    assertTrue(Arrays.equals(new String[] { thisNodeId.toString() }, cel.getLastStringArray()));

    cluster.thisNodeDisconnected();
    assertEquals("thisNodeDisconnected", cel.getLastMethodCalled());
    cel.reset();
    cluster.thisNodeDisconnected();
    assertNull(cel.getLastMethodCalled());
  }

  public void testNodeConnected() {
    // prime cluster
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodeIds);
    cluster.addClusterEventListener(cel);
    assertEquals("thisNodeConnected", cel.getLastMethodCalled());
    assertEquals(thisNodeId.toString(), cel.getLastString());
    assertTrue(Arrays.equals(new String[] { thisNodeId.toString() }, cel.getLastStringArray()));
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

  public void testAddSameListenerTwice() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodesCurrentlyInCluster);

    TestEventListener listener = new TestEventListener();
    cluster.addClusterEventListener(listener);

    assertEquals("thisNodeConnected", listener.getLastMethodCalled());

    listener.reset();
    cluster.addClusterEventListener(listener);

    assertNull(listener.getLastMethodCalled());
  }

  public void testCallbackOnluOnNewListener() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodesCurrentlyInCluster);

    TestEventListener listener = new TestEventListener();
    cluster.addClusterEventListener(listener);

    assertEquals("thisNodeConnected", listener.getLastMethodCalled());

    listener.reset();
    TestEventListener listener2 = new TestEventListener();
    cluster.addClusterEventListener(listener2);

    assertNull(listener.getLastMethodCalled());
    assertEquals("thisNodeConnected", listener2.getLastMethodCalled());
  }

  public void testClientExceptionSafety() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.thisNodeConnected(thisNodeId, nodesCurrentlyInCluster);

    cluster.addClusterEventListener(new TestEventListenerWithExceptions());

    cluster.thisNodeDisconnected();
    cluster.nodeConnected("2");
    cluster.nodeDisconnected(thisNodeId.toString());
  }

  private static class TestEventListener implements ClusterEventListener {

    private String   lastString       = null;
    private String[] lastStringArray  = null;
    private String   lastMethodCalled = null;

    public void nodeConnected(final String nodeId) {
      lastString = nodeId;
      lastMethodCalled = "nodeConnected";
    }

    public void nodeDisconnected(final String nodeId) {
      lastString = nodeId;
      lastMethodCalled = "nodeDisconnected";
    }

    public void thisNodeConnected(final String thisNodeId, final String[] nodesCurrentlyInCluster) {
      lastString = thisNodeId;
      lastStringArray = nodesCurrentlyInCluster;
      lastMethodCalled = "thisNodeConnected";
    }

    public void thisNodeDisconnected(final String thisNodeId) {
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

  private static class TestEventListenerWithExceptions implements ClusterEventListener {

    public void nodeConnected(final String nodeId) {
      throw new RuntimeException("nodeConnected");
    }

    public void nodeDisconnected(final String nodeId) {
      throw new RuntimeException("nodeDisconnected");
    }

    public void thisNodeConnected(final String thisNodeId, final String[] nodesCurrentlyInCluster) {
      throw new RuntimeException("thisNodeConnected");
    }

    public void thisNodeDisconnected(final String thisNodeId) {
      throw new RuntimeException("thisNodeDisconnected");
    }
  }
}