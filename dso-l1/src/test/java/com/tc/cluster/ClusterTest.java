/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.cluster;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.net.ClientID;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tcclient.cluster.ClusterInternal.ClusterEventType;
import com.tcclient.cluster.ClusterInternalEventsContext;
import com.tcclient.cluster.Node;
import com.tcclient.cluster.OutOfBandClusterListener;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class ClusterTest extends TestCase {

  private ClusterImpl cluster;

  @Override
  protected void setUp() throws Exception {
    cluster = new ClusterImpl();
    Stage<ClusterInternalEventsContext> mockStage = Mockito.mock(Stage.class);
    Sink<ClusterInternalEventsContext> mockSink = Mockito.mock(Sink.class);
    Mockito.when(mockStage.getSink()).thenReturn(mockSink);
    Mockito.doAnswer(new Answer<Void>() {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        ClusterInternalEventsContext ce = (ClusterInternalEventsContext) arguments[0];
        cluster.notifyClusterListener(ce.getEventType(), ce.getEvent(), ce.getClusterListener());
        return null;
      }

    }).when(mockSink).addSingleThreaded(Matchers.any(ClusterInternalEventsContext.class));
    cluster.init(mockStage);
  }

  public void testGetThisNode() {
    final ClientID thisNodeId = new ClientID(1);
    cluster.fireThisNodeJoined(thisNodeId, new ClientID[] { new ClientID(2) });
    assertNotNull(cluster.getCurrentNode());
    assertEquals(thisNodeId.toString(), cluster.getCurrentNode().getId());
  }

  public void testGetNodes() {
    // 0
    Collection<Node> nodes = cluster.getClusterTopology().getNodes();
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
    assertEquals(2, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));
    Collection<Node> nodes = cluster.getClusterTopology().getNodes();
    Iterator<Node> nodesIt = nodes.iterator();
    assertTrue(nodesIt.hasNext());
    assertEquals(thisNodeId.toString(), nodesIt.next().getId());
    assertFalse(nodesIt.hasNext());

    // would fire multiple thisNodeConnected events in a row...
    listener.reset();
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);
    assertFalse(listener.getOccurredEvents().isEmpty());
    assertEquals(1, listener.getOccurredEvents().size());

    // but it should not be fired after thisNodeLeft
    cluster.fireThisNodeLeft();
    listener.reset();
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);
    assertTrue(listener.getOccurredEvents().size() == 1);
    assertEquals(1, cluster.getClusterTopology().getNodes().size());

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
    assertTrue(listener.getOccurredEvents().size() == 1);
    assertEquals(1, cluster.getClusterTopology().getNodes().size());

    assertEquals(otherNodeId.toString(), cluster.getCurrentNode().getId());
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

    CountDownLatch startLatch = new CountDownLatch(1);
    TimingRunnable targetRunnable = new TimingRunnable(cluster, thisNodeId, startLatch);
    new Thread(targetRunnable).start();
    while (true)
      try {
        startLatch.await();
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

  public void testOOBNotification() throws Exception {
    final ClientID thisNodeId = new ClientID(1);
    final ClientID[] nodeIds = new ClientID[] { thisNodeId };
    cluster.fireThisNodeJoined(thisNodeId, nodeIds);

    OOBTestListener listener = new OOBTestListener();

    // if this node is connected, adding a listener should result in an immediate callback
    cluster.addClusterListener(listener);
    Thread.sleep(2000);
    assertEquals(2, listener.getOccurredEvents().size());
    assertEquals("ClientID[1] JOINED", listener.getOccurredEvents().get(0));

    cluster.fireOperationsDisabled();
    cluster.fireOperationsEnabled();
    cluster.fireThisNodeLeft();

    Thread.sleep(10000);

    System.out.println("Occured events: " + listener.getOccurredEvents());

    assertEquals(6, listener.getOccurredEvents().size());
    // all events use oob notification, assert contains instead of ordering
    assertTrue(listener.getOccurredEvents().contains("ClientID[1] JOINED"));
    assertTrue(listener.getOccurredEvents().contains("ClientID[1] ENABLED"));
    assertTrue(listener.getOccurredEvents().contains("ClientID[1] DISABLED"));
    assertTrue(listener.getOccurredEvents().contains("ClientID[1] LEFT"));

    // make sure different thread was used all the time
    Assert.assertNull("Out of band notification didn't happen", listener.getError());
  }

  private static class TestEventListener implements ClusterListener {

    public LinkedList<String> events = new LinkedList<String>();

    public LinkedList<String> getOccurredEvents() {
      return events;
    }

    @Override
    public synchronized void nodeJoined(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + " JOINED");
    }

    @Override
    public synchronized void nodeLeft(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + " LEFT");
    }

    @Override
    public synchronized void operationsEnabled(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + " ENABLED");
    }

    @Override
    public synchronized void operationsDisabled(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + " DISABLED");
    }

    @Override
    public void nodeRejoined(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + " REJOINED");
    }

    @Override
    public void nodeError(ClusterEvent event) {
      check();
      events.add(event.getNode().getId() + "NODE ERROR");
    }

    public void check() {
      // no-op
    }

    public synchronized void reset() {
      events.clear();
    }

  }

  private static class TestEventListenerWithExceptions implements ClusterListener {

    @Override
    public void nodeJoined(ClusterEvent event) {
      throw new RuntimeException("nodeJoined");
    }

    @Override
    public void nodeLeft(ClusterEvent event) {
      throw new RuntimeException("nodeLeft");
    }

    @Override
    public void operationsDisabled(ClusterEvent event) {
      throw new RuntimeException("operationsDisabled");
    }

    @Override
    public void operationsEnabled(ClusterEvent event) {
      throw new RuntimeException("operationsEnabled");
    }

    @Override
    public void nodeRejoined(ClusterEvent event) {
      throw new RuntimeException("rejoined");
    }

    @Override
    public void nodeError(ClusterEvent event) {
      throw new RuntimeException("NODE ERROR");
    }

  }

  private static class TimingRunnable implements Runnable {
    private long                 elapsedTimeMillis = 0;
    private final AtomicBoolean  finished          = new AtomicBoolean(false);
    private final ClusterImpl    cluster;
    private final ClientID       expectedNode;
    private Node                 node;
    private final CountDownLatch startLatch;

    public TimingRunnable(ClusterImpl cluster, ClientID expectedNode, CountDownLatch startLatch) {
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

    @Override
    public void run() {
      long start = System.currentTimeMillis();
      this.startLatch.countDown();
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

    public Node getNode() {
      return node;
    }

  }

  private static class OOBTestListener extends TestEventListener implements OutOfBandClusterListener {

    private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    @Override
    public boolean useOutOfBandNotification(ClusterEventType type, ClusterEvent event) {
      return true;
    }

    @Override
    public void check() {
      if (!Thread.currentThread().getName().contains("Out of band notifier")) {
        error.set(new AssertionError());
      }
    }

    public Throwable getError() {
      return error.get();
    }
  }
}
