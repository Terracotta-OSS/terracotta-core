/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import EDU.oswego.cs.dl.util.concurrent.Latch;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ClusterMetaDataManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.util.concurrent.ThreadUtil;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

public class DsoClusterTest extends TestCase {

  private DsoClusterImpl cluster;

  @Override
  protected void setUp() throws Exception {
    cluster = new DsoClusterImpl();
    Stage mockStage = Mockito.mock(Stage.class);
    Sink mockSink = Mockito.mock(Sink.class);
    Mockito.when(mockStage.getSink()).thenReturn(mockSink);
    Mockito.doAnswer(new Answer<Void>() {

      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        ClusterInternalEventsContext ce = (ClusterInternalEventsContext) arguments[0];
        cluster.notifyDsoClusterListener(ce.getEventType(), ce.getEvent(), ce.getDsoClusterListener());
        return null;
      }

    }).when(mockSink).add(Matchers.any(EventContext.class));
    cluster.init(new MockClusterMetaDataManager(), null, mockStage);
  }

  private final static class MockClusterMetaDataManager implements ClusterMetaDataManager {

    public DNAEncoding getEncoding() {
      return null;
    }

    public Set<?> getKeysForOrphanedValues(TCMap tcMap) {
      return null;
    }

    public Set<NodeID> getNodesWithObject(ObjectID id) {
      return null;
    }

    public Map<ObjectID, Set<NodeID>> getNodesWithObjects(Collection<ObjectID> ids) {
      return null;
    }

    public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node) {
      return null;
    }

    public void setResponse(ThreadID threadId, Object response) {
      // no-op
    }

    public <K> Map<K, Set<NodeID>> getNodesWithKeys(final TCMap tcMap, final Collection<? extends K> keys) {
      return null;
    }

    public <K> Map<K, Set<NodeID>> getNodesWithKeys(final TCServerMap tcMap, final Collection<? extends K> keys) {
      return null;
    }

    public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
      // no-op

    }

    public void pause(NodeID remoteNode, int disconnected) {
      // no-op
    }

    public void shutdown() {
      // no-op
    }

    public void unpause(NodeID remoteNode, int disconnected) {
      // no-op
    }
  }

  public void testGetThisNode() {
    final ClientID thisNodeId = new ClientID(1);
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { new ClientID(2) });
    assertNotNull(cluster.getCurrentNode());
    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());
  }

  public void testGetNodes() {
    // 0
    Collection<DsoNode> nodes = cluster.getClusterTopology().getNodes();
    assertNotNull(nodes);
    assertTrue(nodes.isEmpty());

    // 1
    final ClientID thisNodeId = new ClientID(1);
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { thisNodeId });
    nodes = cluster.getClusterTopology().getNodes();
    assertNotNull(nodes);
    assertEquals(1, nodes.size());
    assertEquals(thisNodeId.toString(), nodes.iterator().next().getId());
  }

  public void testThisNodeJoined() {
    final ClientID thisNodeId = new ClientID(1);
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
    final ClientID thisNodeId = new ClientID(1);
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);
    cluster.fireThisNodeLeft();
    listener.reset();

    final ClientID otherNodeId = new ClientID(2);
    final ClientID[] otherNodeIds = new ClientID[] { otherNodeId };
    cluster.fireThisNodeJoined(otherNodeId, otherNodeIds);
    assertTrue(listener.getOccurredEvents().isEmpty());
    assertTrue(cluster.getClusterTopology().getNodes().isEmpty());

    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());
  }

  public void testThisNodeLeft() {
    final ClientID thisNodeId = new ClientID(1);
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

    // adding listener after node_left fires node_left
    TestEventListener newListener = new TestEventListener();
    cluster.addClusterListener(newListener);
    assertEquals(1, newListener.getOccurredEvents().size());
    assertEquals("ClientID[1] LEFT", newListener.getOccurredEvents().get(0));
  }

  public void testNodeConnected() {
    // prime cluster
    final ClientID thisNodeId = new ClientID(1);
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    TestEventListener listener = new TestEventListener();
    cluster.addClusterListener(listener);

    // now cause node joined event
    listener.reset();
    final ClientID otherNodeId = new ClientID(2);
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
    final ClientID thisNodeId = new ClientID(1);
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
    final ClientID thisNodeId = new ClientID(1);
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
    final ClientID thisNodeId = new ClientID(1);
    final ClientID[] nodesCurrentlyInCluster = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodesCurrentlyInCluster);

    cluster.addClusterListener(new TestEventListenerWithExceptions());

    cluster.fireOperationsEnabled();
    cluster.fireNodeJoined(new ClientID(2));
    cluster.fireNodeLeft(new ClientID(2));
    cluster.fireOperationsDisabled();
    cluster.fireThisNodeLeft();
  }

  public void testWaitUntilNodeJoinsCluster() {
    final ClientID thisNodeId = new ClientID(1);

    Latch startLatch = new Latch();
    TimingRunnable targetRunnable = new TimingRunnable(cluster, thisNodeId, startLatch);
    new Thread(targetRunnable).start();
    while (true)
      try {
        startLatch.acquire();
        break;
      } catch (InterruptedException e) {
        System.err.println("XXX startLatch acquire exception : " + e);
      }
    ThreadUtil.reallySleep(2000);
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { new ClientID(2) });
    assertNotNull(cluster.getCurrentNode());
    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());

    targetRunnable.waitUntilFinished();
    assertTrue("waitUntilNodeJoinsCluster must return after node joins", targetRunnable.isFinished());
    assertTrue("Waiting thread should return after approx. 2 secs (actual:" + targetRunnable.getElapsedTimeMillis()
               + ")", targetRunnable.getElapsedTimeMillis() + 100 >= 2000);
    assertEquals("DsoNode returned from waitUntilNodeJoinsCluster should be same as cluster.getCurrentNode",
                 cluster.getCurrentNode(), targetRunnable.getNode());
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

  private static class TimingRunnable implements Runnable {
    private long                 elapsedTimeMillis = 0;
    private final AtomicBoolean  finished          = new AtomicBoolean(false);
    private final DsoClusterImpl cluster;
    private final ClientID       expectedNode;
    private DsoNode              node;
    private final Latch          startLatch;

    public TimingRunnable(DsoClusterImpl cluster, ClientID expectedNode, Latch startLatch) {
      super();
      this.cluster = cluster;
      this.expectedNode = expectedNode;
      this.startLatch = startLatch;
    }

    public void waitUntilFinished() {
      while (!finished.get()) {
        System.out
            .println(Thread.currentThread().getName()
                     + ": waiting until other thread returns from waitUntilNodeJoinsCluster(). Sleeping for 1 sec...");
        ThreadUtil.reallySleep(1000);
      }
      System.out.println(Thread.currentThread().getName() + ": target runnable finished");
    }

    public void run() {
      long start = System.currentTimeMillis();
      this.startLatch.release();
      node = cluster.waitUntilNodeJoinsCluster();
      assertEquals(expectedNode.toString(), node.getId());
      elapsedTimeMillis = System.currentTimeMillis() - start;
      finished.set(true);
    }

    public long getElapsedTimeMillis() {
      return elapsedTimeMillis;
    }

    public boolean isFinished() {
      return finished.get();
    }

    public DsoNode getNode() {
      return node;
    }

  }
}