/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.state;

import com.tc.test.TCTestCase;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManagerImpl;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.State;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.*;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.MockSink;
import com.tc.net.groups.GroupManagerFactory;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.TCThreadGroup;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class StateManagerTest extends TCTestCase {

  private static final TCLogger logger = TCLogging.getLogger(StateManagerImpl.class);

  public StateManagerTest() {
    // disableAllUntil("2007-05-23");
  }

  public void testStateManagerTwoServers() throws Exception {
    // 2 nodes join concurrently
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(2);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerTwoServers failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerThreeServers() throws Exception {
    // 3 nodes join concurrently
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(3);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerThreeServers failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerSixServers() throws Exception {
    // 6 nodes join concurrently
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesConcurrentJoining(6);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerSixServers failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerMixJoinAndElect3() throws Exception {
    // 3 nodes mix join and election
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesMixJoinAndElect(3);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerMixJoinAndElect3 failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerMixJoinAndElect6() throws Exception {
    // 6 nodes mix join and election
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesMixJoinAndElect(6);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerMixJoinAndElect6 failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerJoinLater3() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesJoinLater(3);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerJoinLater3 failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  public void testStateManagerJoinLater6() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    TCThreadGroup threadGroup = new TCThreadGroup(new ThrowableHandler(logger), "StateManagerTestGroup");
    // setup throwable ThreadGroup to catch AssertError from Tribes threads.
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      public void run() {
        try {
          nodesJoinLater(6);
        } catch (Exception e) {
          throw new RuntimeException("testStateManagerJoinLater6 failed!");
        }
      }
    });
    throwableThread.start();
    throwableThread.join();
  }

  // -----------------------------------------------------------------------

  private void nodesConcurrentJoining(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes join at same time.");

    TribesGroupManager[] groupMgr = new TribesGroupManager[nodes];
    ChangeSink[] sinks = new ChangeSink[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandomPort();
      allNodes[i] = new Node("localhost", ports[i]);
    }

    StateManager[] managers = new StateManager[nodes];
    ElectionThread[] elections = new ElectionThread[nodes];
    L2StateMessageStage[] msgStage = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStage);
      elections[i] = new ElectionThread(managers[i]);
    }

    // joining
    System.out.println("*** Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], allNodes);
    }

    System.out.println("*** Start Election...");
    // run them concurrently
    for (int i = 0; i < nodes; ++i) {
      elections[i].start();
    }
    for (int i = 0; i < nodes; ++i) {
      elections[i].join();
    }

    Thread.sleep(500);
    // verification
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertTrue(activeCount == 1);

    shutdown(groupMgr, msgStage);
  }

  private void shutdown(TribesGroupManager[] groupMgr, L2StateMessageStage[] msgStage) throws Exception {
    // shut them down
    shutdown(groupMgr, msgStage, 0, groupMgr.length);
  }

  private void shutdown(TribesGroupManager[] groupMgr, L2StateMessageStage[] msgStage, int start, int end)
      throws Exception {
    for (int i = start; i < end; ++i) {
      groupMgr[i].stop();
      msgStage[i].requestStop();
    }
    Thread.sleep(1000);
  }

  private void nodesMixJoinAndElect(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes mixed join and election at same time.");

    TribesGroupManager[] groupMgr = new TribesGroupManager[nodes];
    ChangeSink[] sinks = new ChangeSink[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandomPort();
      allNodes[i] = new Node("localhost", ports[i]);
    }

    StateManager[] managers = new StateManager[nodes];
    ElectionThread[] elections = new ElectionThread[nodes];
    L2StateMessageStage[] msgStage = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStage);
      elections[i] = new ElectionThread(managers[i]);
    }

    // Joining and Electing
    System.out.println("*** Start Joining and Electing...");
    groupMgr[0].join(allNodes[0], allNodes);
    for (int i = 0; i < nodes - 1; ++i) {
      elections[i].start();
      groupMgr[i + 1].join(allNodes[i + 1], allNodes);
    }
    elections[nodes - 1].start();

    for (int i = 0; i < nodes; ++i) {
      elections[i].join();
    }

    Thread.sleep(500);
    // verification
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertTrue(activeCount == 1);

    shutdown(groupMgr, msgStage);
  }

  private void nodesJoinLater(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes join at later time.");

    NodeID[] ids = new NodeID[nodes];
    ChangeSink[] sinks = new ChangeSink[nodes];
    TribesGroupManager[] groupMgr = new TribesGroupManager[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandomPort();
      allNodes[i] = new Node("localhost", ports[i]);
    }

    final StateManager[] managers = new StateManager[nodes];
    ElectionThread[] elections = new ElectionThread[nodes];
    L2StateMessageStage[] msgStage = new L2StateMessageStage[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, groupMgr, msgStage);
      elections[i] = new ElectionThread(managers[i]);
    }

    // the first node to be the active one
    System.out.println("*** First node joins to be an active node...");
    ids[0] = groupMgr[0].join(allNodes[0], allNodes);
    managers[0].startElection();
    Thread.sleep(100);

    // move following join nodes to passive-standby
    groupMgr[0].registerForGroupEvents(new MyGroupEventListener() {
      public void nodeJoined(NodeID nodeID) {
        System.out.println("*** moveNodeToPassiveStandby -> " + nodeID.getName());
        managers[0].moveNodeToPassiveStandby(nodeID);
        // managers[0].publishActiveState(nodeID);
      }
    });

    System.out.println("***  Remaining nodes join");
    for (int i = 1; i < nodes; ++i) {
      ids[i] = groupMgr[i].join(allNodes[i], allNodes);
    }

    Thread.sleep(1000);
    // verification: first node must be active
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertTrue((activeCount == 1) && managers[0].isActiveCoordinator());

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
    shutdown(groupMgr, msgStage,0,1);

    ElectionIfNecessaryThread reElectThreads[] = new ElectionIfNecessaryThread[nodes];
    for (int i = 1; i < nodes; ++i) {
      reElectThreads[i] = new ElectionIfNecessaryThread(managers[i], ids[0]);
    }
    for (int i = 1; i < nodes; ++i) {
      reElectThreads[i].start();
    }
    for (int i = 1; i < nodes; ++i) {
      reElectThreads[i].join();
    }
    Thread.sleep(500);

    // verify
    activeCount = 0;
    for (int i = 1; i < nodes; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] (" + (active ? "active" : "non-active") + ")state is " + sinks[i]);
    }
    assertTrue(activeCount == 1);

    // shut them down
    shutdown(groupMgr, msgStage, 1, nodes);
  }

  private StateManager createStateManageNode(int localIndex, ChangeSink[] sinks, TribesGroupManager[] groupMgr,
                                             L2StateMessageStage[] messageStage) throws Exception {

    TribesGroupManager gm = (TribesGroupManager) GroupManagerFactory.createGroupManager();
    groupMgr[localIndex] = gm;
    MyGroupEventListener gel = new MyGroupEventListener();
    MyListener l = new MyListener();
    gm.registerForMessages(TestMessage.class, l);
    gm.registerForGroupEvents(gel);
    sinks[localIndex] = new ChangeSink(localIndex);
    MyStateManagerConfig config = new MyStateManagerConfig();
    config.electionTime = 5;
    StateManager mgr = new StateManagerImpl(logger, gm, sinks[localIndex], config, WeightGeneratorFactory.createDefaultFactory());
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

    public void requestStop() {
      stop = true;
    }

    public Sink getSink() {
      return sink;
    }

    public void run() {
      while (!stop) {
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

    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### nodeJoined -> " + nodeID.getName());
      lastNodeJoined = nodeID;
    }

    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### nodeLeft -> " + nodeID.getName());
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

    String msg;

    protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
      msg = in.readUTF();

    }

    protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
      out.writeUTF(msg);

    }

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
