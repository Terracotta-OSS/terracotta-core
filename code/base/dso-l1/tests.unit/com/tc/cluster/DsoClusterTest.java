/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.test.TCTestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class DsoClusterTest extends TCTestCase {

  private DsoClusterImpl cluster;

  @Override
  protected void setUp() throws Exception {
    cluster = new DsoClusterImpl();
  }

  public void testGetThisNode() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { new ClientID(new ChannelID(2)) });
    assertNotNull(cluster.getCurrentNode());
    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());
  }

  public void testGetNodes() {
    // 0
    Collection<DsoNode> nodes = cluster.getClusterTopology().getNodes();
    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());

    // 1
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { thisNodeId });
    nodes = cluster.getClusterTopology().getNodes();
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertEquals(thisNodeId.toString(), nodes.iterator().next().getId());
  }

  public void testThisNodeJoined() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    TestEventListener listener = new TestEventListener();

    // if this node is connected, adding a listener should result in an immediate callback
    cluster.addClusterListener(listener);
    assertEquals(1, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));
    Collection<DsoNode> nodes = cluster.getClusterTopology().getNodes();
    Iterator<DsoNode> nodesIt = nodes.iterator();
    assertTrue(nodesIt.hasNext());
    assertEquals(thisNodeId.toString(), nodesIt.next().getId());
    assertFalse(nodesIt.hasNext());

    // should not fire multiple thisNodeConnected events in a row...
    listener.reset();
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);
    assertTrue(listener.getOccurredEvents().isEmpty());

    // but it should not be fired after thisNodeLeft
    cluster.fireThisNodeLeft();
    listener.reset();
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);
    assertTrue(listener.getOccurredEvents().isEmpty());
    assertTrue(cluster.getClusterTopology().getNodes().isEmpty());

    // no callback if listener is added again
    listener.reset();
    cluster.addClusterListener(listener);
    assertTrue(listener.getOccurredEvents().isEmpty());
  }

  public void testNodeIDCantChange() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);
    cluster.fireThisNodeLeft();
    listener.reset();

    final ClientID otherNodeId = new ClientID(new ChannelID(2));
    final ClientID[] otherNodeIds = new ClientID[] { otherNodeId };
    cluster.fireThisNodeJoined(otherNodeId, otherNodeIds);
    assertTrue(listener.getOccurredEvents().isEmpty());
    assertTrue(cluster.getClusterTopology().getNodes().isEmpty());

    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());
  }

  public void testThisNodeLeft() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);
    cluster.fireOperationsEnabled();

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);

    cluster.fireThisNodeLeft();
    assertEquals(4, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));
    assertEquals("ClientID[1] ENABLED", listener.getOccurredEvents().get(1));
    assertEquals("ClientID[1] DISABLED", listener.getOccurredEvents().get(2));
    assertEquals("ClientID[1] LEFT", listener.getOccurredEvents().get(3));
    listener.reset();
    cluster.fireThisNodeLeft();
    assertTrue(listener.getOccurredEvents().isEmpty());
  }

  public void testNodeConnected() {
    // prime cluster
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);

    // now cause node joined event
    listener.reset();
    final ClientID otherNodeId = new ClientID(new ChannelID(2));
    cluster.fireNodeJoined(otherNodeId);
    assertEquals(1, listener.getOccurredEvents().size());
    assertEquals("ClientID[2] JOINED", listener.getOccurredEvents().get(0));

    // now cause node left event
    listener.reset();
    cluster.fireNodeLeft(otherNodeId);
    assertEquals(1, listener.getOccurredEvents().size());
    assertEquals("ClientID[2] LEFT", listener.getOccurredEvents().get(0));
  }

  public void testAddSameListenerTwice() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodesCurrentlyInCluster);
    cluster.fireOperationsEnabled();

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);
    cluster.addClusterListener(listener);

    cluster.fireThisNodeLeft();

    assertEquals(4, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));
    assertEquals("ClientID[1] ENABLED", listener.getOccurredEvents().get(1));
    assertEquals("ClientID[1] DISABLED", listener.getOccurredEvents().get(2));
    assertEquals("ClientID[1] LEFT", listener.getOccurredEvents().get(3));
  }

  public void testCallbackOnlyOnNewListener() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodesCurrentlyInCluster);
    cluster.fireOperationsEnabled();

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);

    assertEquals(2, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));
    assertEquals("ClientID[1] ENABLED", listener.getOccurredEvents().get(1));

    listener.reset();
    TestEventListener listener2 = new TestEventListener();
    cluster.addClusterListener(listener2);

    assertTrue(listener.getOccurredEvents().isEmpty());
    assertEquals(2, listener2.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener2.getOccurredEvents().get(0));
    assertEquals("ClientID[1] ENABLED", listener2.getOccurredEvents().get(1));
  }

  public void testClientExceptionSafety() {
    final ClientID thisNodeId = new ClientID(new ChannelID(1));
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodesCurrentlyInCluster);

    cluster.addClusterListener(new TestEventListenerWithExceptions());

    cluster.fireOperationsEnabled();
    cluster.fireNodeJoined(new ClientID(new ChannelID(2)));
    cluster.fireNodeLeft(new ClientID(new ChannelID(2)));
    cluster.fireOperationsDisabled();
    cluster.fireThisNodeLeft();
  }

  private static class TestEventListener implements DsoClusterListener {

    public LinkedList<String> events = new LinkedList<String>();

    public LinkedList<String> getOccurredEvents() {
      return events;
    }

    public synchronized void nodeJoined(final DsoClusterEvent event) {
      events.add(event.getNode().getId() + " JOINED");
    }

    public synchronized void nodeLeft(final DsoClusterEvent event) {
      events.add(event.getNode().getId() + " LEFT");
    }

    public synchronized void operationsEnabled(final DsoClusterEvent event) {
      events.add(event.getNode().getId() + " ENABLED");
    }

    public synchronized void operationsDisabled(final DsoClusterEvent event) {
      events.add(event.getNode().getId() + " DISABLED");
    }

    public synchronized void reset() {
      events.clear();
    }
  }

  private static class TestEventListenerWithExceptions implements DsoClusterListener {

    public void nodeJoined(final DsoClusterEvent event) {
      throw new RuntimeException("nodeJoined");
    }

    public void nodeLeft(final DsoClusterEvent event) {
      throw new RuntimeException("nodeLeft");
    }

    public void operationsDisabled(final DsoClusterEvent event) {
      throw new RuntimeException("operationsDisabled");
    }

    public void operationsEnabled(final DsoClusterEvent event) {
      throw new RuntimeException("operationsEnabled");
    }
  }
}