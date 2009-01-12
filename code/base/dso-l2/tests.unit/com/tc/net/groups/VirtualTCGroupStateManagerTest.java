/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.StageManagerImpl;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfig;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.State;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class VirtualTCGroupStateManagerTest extends TCTestCase {

  private final static String   LOCALHOST = "localhost";
  private static final TCLogger logger    = TCLogging.getLogger(VirtualTCGroupStateManagerTest.class);
  private TCThreadGroup         threadGroup;

  public VirtualTCGroupStateManagerTest() {
    // disableAllUntil("2009-01-14");
  }

  public void setUp() {
    threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "VirtualTCGroupStateManagerTestGroup");
  }

  public void testStateManagerTwoServers() throws Exception {
    // 2 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(4, 2);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerTwoServers failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerThreeServers() throws Exception {
    // 3 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(6, 3);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerThreeServers failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerSixServers() throws Exception {
    // 6 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(8, 6);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerSixServers failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerMixJoinAndElect3() throws Exception {
    // 3 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesMixJoinAndElect(6, 3);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerMixJoinAndElect3 failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerMixJoinAndElect6() throws Exception {
    // 6 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesMixJoinAndElect(8, 6);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerMixJoinAndElect6 failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerJoinLater3() throws Exception {
    // first node shall be active and remaining 2 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesJoinLater(6, 3);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerJoinLater3 failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerJoinLater6() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesJoinLater(9, 6);
        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testStateManagerJoinLater6 failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  // -----------------------------------------------------------------------

  private void nodesConcurrentJoining(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at same time.");

    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1, TCSocketAddress.WILDCARD_IP);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], virtualNodes);
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }

    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // joining
    System.out.println("*** Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], allNodes);
    }
    ThreadUtil.reallySleep(1000 * nodes);

    System.out.println("*** Start Election...");
    // run them concurrently
    for (int i = 0; i < virtuals; ++i) {
      elections[i].start();
    }
    for (int i = 0; i < virtuals; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);
    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    stopMessageStages(msgStages);
    shutdown(groupMgr);
  }

  private void stopMessageStages(L2StateMessageStage[] msgStages) {
    stopMessageStages(msgStages, 0, msgStages.length);
  }

  private void stopMessageStages(L2StateMessageStage[] msgStages, int start, int end) {
    for (int i = start; i < end; ++i) {
      msgStages[i].requestStop();
    }
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr) {
    // shut them down
    shutdown(groupMgr, 0, groupMgr.length);
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr, int start, int end) {
    for (int i = start; i < end; ++i) {
      try {
        ThreadUtil.reallySleep(100);
        groupMgr[i].stop(1000);
      } catch (Exception ex) {
        System.out.println("*** Failed to stop Server[" + i + "] " + groupMgr[i] + " " + ex);
      }
    }
    System.out.println("*** shutdown done");
  }

  private void nodesMixJoinAndElect(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals
                       + " nodes mixed join and election at same time.");

    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1, TCSocketAddress.WILDCARD_IP);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], virtualNodes);
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }
    
    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // Joining and Electing
    System.out.println("*** Start Joining and Electing...");
    groupMgr[0].join(allNodes[0], allNodes);
    elections[0].start();
    for (int i = 1; i < virtuals; ++i) {
      groupMgr[i].join(allNodes[i], allNodes);
      elections[i].start();
    }
    for (int i = virtuals + 1; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], allNodes);
    }

    for (int i = 0; i < virtuals; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);
    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    stopMessageStages(msgStages);
    shutdown(groupMgr);
  }

  private void nodesJoinLater(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at later time.");

    final LinkedBlockingQueue<NodeID> joinedNodes = new LinkedBlockingQueue<NodeID>();
    NodeID[] ids = new NodeID[nodes];
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1, TCSocketAddress.WILDCARD_IP);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], virtualNodes);
    }

    ChangeSink[] sinks = new ChangeSink[nodes];
    final StateManager[] managers = new StateManager[nodes];
    L2StateMessageStage[] msgStages = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStages);
    }
    
    ElectionThread[] elections = new ElectionThread[virtuals];
    for (int i = 0; i < virtuals; ++i) {
      elections[i] = new ElectionThread(managers[i]);
    }

    // the first node to be the active one
    System.out.println("*** First node joins to be an active node...");
    ids[0] = groupMgr[0].join(allNodes[0], allNodes);
    managers[0].startElection();
    ThreadUtil.reallySleep(100);

    // move following join nodes to passive-standby
    virtualMgr[0].registerForGroupEvents(new MyGroupEventListener(virtualMgr[0].getLocalNodeID()) {
      public void nodeJoined(NodeID nodeID) {
        // save nodeID for moving to passive
        joinedNodes.add(nodeID);
      }
    });

    System.out.println("***  Remaining nodes join");
    for (int i = 1; i < nodes; ++i) {
      ids[i] = groupMgr[i].join(allNodes[i], allNodes);
    }

    ThreadUtil.reallySleep(1000);
    int nodesNeedToMoveToPassive = virtuals - 1;
    while (nodesNeedToMoveToPassive > 0) {
      NodeID toBePassiveNode = joinedNodes.poll(5000, TimeUnit.MILLISECONDS);
      System.out.println("*** moveNodeToPassiveStandby -> " + toBePassiveNode);
      managers[0].moveNodeToPassiveStandby(toBePassiveNode);
      --nodesNeedToMoveToPassive;
    }
    assertTrue(nodesNeedToMoveToPassive == 0);

    ThreadUtil.reallySleep(1000 * nodes);
    // verification: first node must be active
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);
    assertTrue("Node-0 must be active coordinator", managers[0].isActiveCoordinator());

    // check API
    try {
      // active is supported not to move itself to passive stand-by
      managers[0].moveNodeToPassiveStandby(ids[0]);
      throw new RuntimeException("moveNodeToPassiveStandy expected to trows an expection");
    } catch (Exception x) {
      // expected
    }

    System.out.println("*** Stop active and re-elect");
    // stop active node
    stopMessageStages(msgStages, 0, 1);
    shutdown(groupMgr, 0, 1);

    ElectionIfNecessaryThread reElectThreads[] = new ElectionIfNecessaryThread[virtuals];
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i] = new ElectionIfNecessaryThread(managers[i], ids[0]);
    }
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i].start();
    }
    for (int i = 1; i < virtuals; ++i) {
      reElectThreads[i].join();
    }
    ThreadUtil.reallySleep(1000);

    // verify
    activeCount = 0;
    for (int i = 1; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] (" + (active ? "active" : "non-active") + ")state is " + sinks[i]);
    }
    assertEquals("Active coordinator", 1, activeCount);

    // shut them down
    stopMessageStages(msgStages, 1, virtuals);
    shutdown(groupMgr, 1, nodes);
  }

  private TCGroupManagerImpl createTCGroupManager(Node node) throws Exception {
    StageManager stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(), node.getPort(), node
        .getGroupPort(), stageManager);
    ConfigurationContext context = new ConfigurationContextImpl(stageManager);
    stageManager.startAll(context, Collections.EMPTY_LIST);
    return gm;
  }

  private StateManager createStateManageNode(int localIndex, ChangeSink[] sinks, TCGroupManagerImpl[] groupMgr,
                                             L2StateMessageStage[] messageStage) throws Exception {
    TCGroupManagerImpl gm = groupMgr[localIndex];
    gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm));

    MyGroupEventListener gel = new MyGroupEventListener(gm.getLocalNodeID());
    MyListener l = new MyListener();
    gm.registerForMessages(TestMessage.class, l);
    gm.registerForGroupEvents(gel);
    sinks[localIndex] = new ChangeSink(localIndex);
    MyStateManagerConfig config = new MyStateManagerConfig();
    config.electionTime = 5;
    StateManager mgr = new StateManagerImpl(logger, gm, sinks[localIndex], config, WeightGeneratorFactory
        .createDefaultFactory());
    messageStage[localIndex] = new L2StateMessageStage(mgr);
    gm.routeMessages(L2StateMessage.class, messageStage[localIndex].getSink());
    messageStage[localIndex].start();
    return (mgr);
  }

  private static class L2StateMessageStage extends Thread {
    private final MockSink               sink;
    private final NoExceptionLinkedQueue processQ = new NoExceptionLinkedQueue();
    private final StateManager           mgr;
    private volatile boolean             stop     = false;

    public L2StateMessageStage(StateManager mgr) {
      this.mgr = mgr;
      this.sink = new MockSink() {
        public void add(EventContext ec) {
          processQ.put(ec);
        }
      };
      setDaemon(true);
      setName("L2StateMessageStageThread");
    }

    public synchronized void requestStop() {
      stop = true;
    }

    public synchronized boolean isStopped() {
      return stop;
    }

    public Sink getSink() {
      return sink;
    }

    public void run() {
      while (!isStopped()) {
        L2StateMessage m = (L2StateMessage) processQ.poll(3000);
        if (m != null) {
          mgr.handleClusterStateMessage(m);
        }
      }
    }
  }

  private static class ElectionThread extends Thread {
    private StateManager mgr;

    public ElectionThread(StateManager mgr) {
      setMgr(mgr);
    }

    public void setMgr(StateManager mgr) {
      this.mgr = mgr;
    }

    public void run() {
      mgr.startElection();
    }
  }

  private static class MyStateManagerConfig implements StateManagerConfig {
    public int electionTime;

    public int getElectionTimeInSecs() {
      return electionTime;
    }
  }

  private static class ElectionIfNecessaryThread extends Thread {
    private StateManager mgr;
    private NodeID       disconnectedNode;

    public ElectionIfNecessaryThread(StateManager mgr, NodeID disconnectedNode) {
      this.mgr = mgr;
      this.disconnectedNode = disconnectedNode;
    }

    public void run() {
      mgr.startElectionIfNecessary(disconnectedNode);
    }
  }

  private static class ChangeSink extends MockSink {
    private int               serverIndex;
    private StateChangedEvent event = null;

    public ChangeSink(int index) {
      serverIndex = index;
    }

    public void add(EventContext context) {
      event = (StateChangedEvent) context;
      System.out.println("*** Server[" + serverIndex + "]: " + event);
    }

    public State getState() {
      if (event == null) return null;
      return event.getCurrentState();
    }

    public String toString() {
      State st = getState();
      return ((st != null) ? st.toString() : "<state unknown>");
    }

  }

  private static class MyGroupEventListener implements GroupEventsListener {

    private NodeID lastNodeJoined;
    private NodeID lastNodeLeft;
    private NodeID gmNodeID;

    public MyGroupEventListener(NodeID nodeID) {
      this.gmNodeID = nodeID;
    }

    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeJoined -> " + nodeID);
      lastNodeJoined = nodeID;
    }

    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeLeft -> " + nodeID);
      lastNodeLeft = nodeID;
    }

    public NodeID getLastNodeJoined() {
      return lastNodeJoined;
    }

    public NodeID getLastNodeLeft() {
      return lastNodeLeft;
    }

    public void reset() {
      lastNodeJoined = lastNodeLeft = null;
    }
  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue queue = new NoExceptionLinkedQueue();

    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
    }

    public GroupMessage take() {
      return (GroupMessage) queue.take();
    }

  }

  private static final class TestMessage extends AbstractGroupMessage {

    // to make serialization sane
    public TestMessage() {
      super(0);
    }

    public TestMessage(String message) {
      super(0);
      this.msg = message;
    }

    protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
      msg = in.readString();
    }

    protected void basicSerializeTo(TCByteBufferOutput out) {
      out.writeString(msg);
    }

    String msg;

    public int hashCode() {
      return msg.hashCode();
    }

    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }

}
