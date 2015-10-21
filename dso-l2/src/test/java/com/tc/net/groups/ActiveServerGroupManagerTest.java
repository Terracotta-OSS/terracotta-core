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
package com.tc.net.groups;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.StageManagerImpl;
import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.Enrollment;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfig;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.net.groups.HaConfigForGroupNameTests;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.State;
import com.tc.util.UUID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class ActiveServerGroupManagerTest extends TCTestCase {

  private final static String            LOCALHOST             = "localhost";
  private static final TCLogger          logger                = TCLogging
                                                                   .getLogger(ActiveServerIDStateManagerTest.class);
  private TCThreadGroup                  threadGroup;

  private TCGroupManagerImpl[]           groupMgrs             = null;
  private Node[]                         allNodes              = null;
  private ChangeSink[]                   sinks                 = null;
  private StateManager[]                 managers              = null;
  private ElectionThread[]               elections             = null;
  private L2StateMessageStage[]          msgStages             = null;
  private ActiveServerGroupManagerImpl[] activeServerGroupMgrs = null;
  private ActiveServerIDManager[]        serverIDManagers      = null;
  private Map<GroupID, ServerID>         groupIDServerIDMap    = null;
  private Map<GroupID, Set<Node>>        groupNodesMap         = null;
  private MyGroupEventListener[]         eventListeners        = null;
  private MyListener[]                   listeners             = null;

  public ActiveServerGroupManagerTest() {
    // disableAllUntil("2009-01-14");
  }

  @Override
  public void setUp() {
    threadGroup = new TCThreadGroup(new ThrowableHandlerImpl(logger), "VirtualTCGroupStateManagerTestGroup");
  }

  public void testSendAll() throws Exception {
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        try {

          Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
          GroupID gid1 = new GroupID(1);
          GroupID gid2 = new GroupID(2);
          GroupID gid3 = new GroupID(3);
          GroupID gid4 = new GroupID(4);
          nodesGroupMap.put(new Integer(0), gid1);
          nodesGroupMap.put(new Integer(1), gid1);
          nodesGroupMap.put(new Integer(2), gid2);
          nodesGroupMap.put(new Integer(3), gid2);
          nodesGroupMap.put(new Integer(4), gid3);
          nodesGroupMap.put(new Integer(5), gid3);
          nodesGroupMap.put(new Integer(6), gid4);
          nodesGroupMap.put(new Integer(7), gid4);

          int nodes = 8;

          initiate(nodes, nodesGroupMap);
          groupJoining(nodes);
          verify(nodes, nodesGroupMap);

          // key parts of testing
          for (int sender = 0; sender < nodes; ++sender) {
            AbstractGroupMessage msg = new TestMessage("test-1-");
            System.out.println("*** Write message to all from Server[" + sender + "]");
            activeServerGroupMgrs[sender].sendAll(msg);

            // not send to itself
            assertTrue(listeners[sender].isEmpty());

            for (int i = 0; i < nodes; ++i) {
              if ((i != sender) && managers[i].isActiveCoordinator()) {
                System.out.println("*** Read message from Server[" + i + "]");
                GroupMessage msgBack = listeners[i].take();
                assertEquals("TestMessage", msg, msgBack);
              }
              assertTrue(listeners[i].isEmpty());
            }
          }

          stopMessageStages();
          shutdown(groupMgrs);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testSendAll failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();

  }

  public void testSendTo() throws Exception {
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        try {

          Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
          GroupID gid1 = new GroupID(1);
          GroupID gid2 = new GroupID(2);
          GroupID gid3 = new GroupID(3);
          nodesGroupMap.put(new Integer(0), gid1);
          nodesGroupMap.put(new Integer(1), gid1);
          nodesGroupMap.put(new Integer(2), gid2);
          nodesGroupMap.put(new Integer(3), gid2);
          nodesGroupMap.put(new Integer(4), gid3);
          nodesGroupMap.put(new Integer(5), gid3);

          int nodes = 6;

          initiate(nodes, nodesGroupMap);
          groupJoining(nodes);
          verify(nodes, nodesGroupMap);

          // key parts of testing
          int sender = 0;
          AbstractGroupMessage msg = new TestMessage("test-2-");
          System.out.println("*** Write message to GroupID[2] from Server[" + sender + "]");
          activeServerGroupMgrs[sender].sendTo(gid2, msg);
          System.out.println("*** Write message to GroupID[3] from Server[" + sender + "]");
          activeServerGroupMgrs[sender].sendTo(gid3, msg);
          for (int i = 2; i < 6; ++i) {
            if ((i != sender) && managers[i].isActiveCoordinator()) {
              System.out.println("*** Read message from Server[" + i + "]");
              GroupMessage msgBack = listeners[i].take();
              assertEquals("TestMessage", msg, msgBack);
            }
            assertTrue(listeners[i].isEmpty());
          }

          stopMessageStages();
          shutdown(groupMgrs);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testSendTo failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();

  }

  public void testSendAllSet() throws Exception {
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        try {

          Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
          GroupID gid1 = new GroupID(1);
          GroupID gid2 = new GroupID(2);
          GroupID gid3 = new GroupID(3);
          GroupID gid4 = new GroupID(4);
          nodesGroupMap.put(new Integer(0), gid1);
          nodesGroupMap.put(new Integer(1), gid1);
          nodesGroupMap.put(new Integer(2), gid2);
          nodesGroupMap.put(new Integer(3), gid2);
          nodesGroupMap.put(new Integer(4), gid3);
          nodesGroupMap.put(new Integer(5), gid3);
          nodesGroupMap.put(new Integer(6), gid4);
          nodesGroupMap.put(new Integer(7), gid4);

          int nodes = 8;

          initiate(nodes, nodesGroupMap);
          groupJoining(nodes);
          verify(nodes, nodesGroupMap);

          // key parts of testing
          Set<GroupID> groupIDSet = new HashSet<>();
          groupIDSet.add(gid1);

          for (int sender = 2; sender < nodes; ++sender) {
            AbstractGroupMessage msg = new TestMessage("test-1-");
            System.out.println("*** Write message to all from Server[" + sender + "]");
            activeServerGroupMgrs[sender].sendAll(msg, groupIDSet);

            for (int i = 0; i < 2; ++i) {
              if ((i != sender) && managers[i].isActiveCoordinator()) {
                System.out.println("*** Read message from Server[" + i + "]");
                GroupMessage msgBack = listeners[i].take();
                assertEquals("TestMessage", msg, msgBack);
              }
              assertTrue(listeners[i].isEmpty());
            }
          }

          stopMessageStages();
          shutdown(groupMgrs);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testSendAll failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();

  }

  public void testSendToAndWaitForResponse() throws Exception {
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        try {

          Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
          GroupID gid1 = new GroupID(1);
          GroupID gid2 = new GroupID(2);
          GroupID gid3 = new GroupID(3);
          nodesGroupMap.put(new Integer(0), gid1);
          nodesGroupMap.put(new Integer(1), gid1);
          nodesGroupMap.put(new Integer(2), gid2);
          nodesGroupMap.put(new Integer(3), gid2);
          nodesGroupMap.put(new Integer(4), gid3);
          nodesGroupMap.put(new Integer(5), gid3);

          int nodes = 6;

          initiate(nodes, nodesGroupMap);
          groupJoining(nodes);
          verify(nodes, nodesGroupMap);

          ResponseL2StateMessageListener[] responseListeners = new ResponseL2StateMessageListener[nodes];
          for (int i = 0; i < nodes; ++i) {
            responseListeners[i] = new ResponseL2StateMessageListener(groupMgrs[i], 1000);
            activeServerGroupMgrs[i].registerForMessages(L2StateMessage.class, responseListeners[i]);
          }

          // key parts of testing
          for (int sender = 0; sender < nodes; ++sender) {
            if (!managers[sender].isActiveCoordinator()) continue;
            GroupID srcGid = nodesGroupMap.get(new Integer(sender));
            L2StateMessage msg = createL2StateMessage();
            if (srcGid != gid1) {
              System.out.println("*** Write/wait message to " + gid1 + " from Server[" + sender + "]");
              activeServerGroupMgrs[sender].sendToAndWaitForResponse(gid1, msg);
            }
            if (srcGid != gid2) {
              System.out.println("*** Write/wait message to " + gid2 + " from Server[" + sender + "]");
              activeServerGroupMgrs[sender].sendToAndWaitForResponse(gid2, msg);
            }
            if (srcGid != gid3) {
              System.out.println("*** Write/wait message to " + gid3 + " from Server[" + sender + "]");
              activeServerGroupMgrs[sender].sendToAndWaitForResponse(gid3, msg);
            }
            for (int i = 0; i < 6; ++i) {
              if (i != sender && managers[i].isActiveCoordinator()) {
                System.out.println("*** Read message from Server[" + i + "]");
                L2StateMessage msgBack = (L2StateMessage) responseListeners[i]
                    .getNextMessageFrom(activeServerGroupMgrs[sender].getLocalNodeID());
                assertEquals("L2StateMessage.Type", msg.getType(), msgBack.getType());
                assertEquals("L2StateMessage.Enrollment", msg.getEnrollment(), msgBack.getEnrollment());
              }
            }
          }

          stopMessageStages();
          shutdown(groupMgrs);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testsendAllAndWaitForResponse failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();

  }

  public void testsendAllAndWaitForResponse() throws Exception {
    Thread throwableThread = new Thread(threadGroup, new Runnable() {
      @Override
      public void run() {
        try {

          Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
          GroupID gid1 = new GroupID(1);
          GroupID gid2 = new GroupID(2);
          GroupID gid3 = new GroupID(3);
          GroupID gid4 = new GroupID(4);
          nodesGroupMap.put(new Integer(0), gid1);
          nodesGroupMap.put(new Integer(1), gid1);
          nodesGroupMap.put(new Integer(2), gid2);
          nodesGroupMap.put(new Integer(3), gid2);
          nodesGroupMap.put(new Integer(4), gid3);
          nodesGroupMap.put(new Integer(5), gid3);
          nodesGroupMap.put(new Integer(6), gid4);
          nodesGroupMap.put(new Integer(7), gid4);

          int nodes = 8;

          initiate(nodes, nodesGroupMap);
          groupJoining(nodes);
          verify(nodes, nodesGroupMap);

          ResponseL2StateMessageListener[] responseListeners = new ResponseL2StateMessageListener[nodes];
          for (int i = 0; i < nodes; ++i) {
            responseListeners[i] = new ResponseL2StateMessageListener(groupMgrs[i], 1000);
            activeServerGroupMgrs[i].registerForMessages(L2StateMessage.class, responseListeners[i]);
          }

          // key parts of testing
          for (int sender = 0; sender < nodes; ++sender) {
            if (!managers[sender].isActiveCoordinator()) continue;
            L2StateMessage msg = createL2StateMessage();
            System.out.println("*** Write/wait message to all from Server[" + sender + "]");
            activeServerGroupMgrs[sender].sendAllAndWaitForResponse(msg);

            for (int i = 0; i < nodes; ++i) {
              if ((i != sender) && managers[i].isActiveCoordinator()) {
                System.out.println("*** Read message from Server[" + i + "]");
                L2StateMessage msgBack = (L2StateMessage) responseListeners[i]
                    .getNextMessageFrom(activeServerGroupMgrs[sender].getLocalNodeID());
                assertEquals("L2StateMessage.Type", msg.getType(), msgBack.getType());
                assertEquals("L2StateMessage.Enrollment", msg.getEnrollment(), msgBack.getEnrollment());
              }
            }
          }

          stopMessageStages();
          shutdown(groupMgrs);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("testsendAllAndWaitForResponse failed! " + e);
        }
      }
    });
    throwableThread.start();
    throwableThread.join();

  }

  // -----------------------------------------------------------------------

  private void initiate(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    System.out.println("*** Testing total=" + nodes + " nodes join at same time.");

    groupMgrs = new TCGroupManagerImpl[nodes];
    allNodes = new Node[nodes];
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    int[] ports = new int[nodes];
    PortChooser pc = new PortChooser();
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgrs[i] = createTCGroupManager(allNodes[i]);
      groupMgrs[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupMgrs[i]));
    }

    groupNodesMap = new HashMap<>();
    for (int i = 0; i < nodes; ++i) {
      GroupID gid = nodesGroupMap.get(new Integer(i));
      Set<Node> grpNodes = groupNodesMap.get(gid);
      if (grpNodes == null) {
        grpNodes = new HashSet<>();
        groupNodesMap.put(gid, grpNodes);
      }
      grpNodes.add(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgrs = new VirtualTCGroupManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      GroupID gid = nodesGroupMap.get(new Integer(i));
      HashSet<String> names = new HashSet<>();
      for (Node node : groupNodesMap.get(gid)) {
        names.add(node.getServerNodeName());
      }
      virtualMgrs[i] = new VirtualTCGroupManagerImpl(groupMgrs[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    sinks = new ChangeSink[nodes];
    managers = new StateManager[nodes];
    elections = new ElectionThread[nodes];
    msgStages = new L2StateMessageStage[nodes];
    serverIDManagers = new ActiveServerIDManager[nodes];
    activeServerGroupMgrs = new ActiveServerGroupManagerImpl[nodes];
    eventListeners = new MyGroupEventListener[nodes];
    listeners = new MyListener[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, virtualMgrs);
      elections[i] = new ElectionThread(managers[i]);
      serverIDManagers[i] = new ActiveServerIDManagerImpl(managers[i], groupMgrs[i], nodesGroupMap.get(new Integer(i)),
                                                          nodeStore);
      activeServerGroupMgrs[i] = new ActiveServerGroupManagerImpl(serverIDManagers[i], groupMgrs[i]);

      eventListeners[i] = new MyGroupEventListener(activeServerGroupMgrs[i].getLocalNodeID());
      listeners[i] = new MyListener();
      activeServerGroupMgrs[i].registerForMessages(TestMessage.class, listeners[i]);
      activeServerGroupMgrs[i].registerForGroupEvents(eventListeners[i]);

      msgStages[i] = new L2StateMessageStage(managers[i]);
      activeServerGroupMgrs[i].routeMessages(L2StateMessage.class, msgStages[i].getSink());
      msgStages[i].start();

    }

    // allow nodes to be ready
    ThreadUtil.reallySleep(400 * nodes);
  }

  private void groupJoining(int nodes) throws Exception {
    System.out.println("*** Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      Set<Node> nodeSet = new HashSet<>();
      Collections.addAll(nodeSet, allNodes);
      NodesStore nodeStore = new NodesStoreImpl(nodeSet);
      groupMgrs[i].join(allNodes[i], nodeStore);
    }
    ThreadUtil.reallySleep(1000 * nodes);

    System.out.println("*** Start Election...");
    for (int i = 0; i < nodes; ++i) {
      elections[i].start();
    }
    for (int i = 0; i < nodes; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);
  }

  private void verify(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    // verification
    int activeCount = 0;
    groupIDServerIDMap = new HashMap<>();
    for (int i = 0; i < nodes; ++i) {
      if (managers[i].isActiveCoordinator()) {
        ++activeCount;
        groupIDServerIDMap.put(nodesGroupMap.get(new Integer(i)), (ServerID) groupMgrs[i].getLocalNodeID());
      }
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", groupNodesMap.size(), activeCount);

    for (int i = 0; i < nodes; ++i) {
      ActiveServerIDManager serverIDManager = serverIDManagers[i];
      for (GroupID gid : groupIDServerIDMap.keySet()) {
        TestCase.assertEquals("Node[" + i + "] ", groupIDServerIDMap.get(gid), serverIDManager.getActiveServerIDFor(gid));
      }
    }
  }

  private void stopMessageStages() {
    stopMessageStages(0, msgStages.length);
  }

  private void stopMessageStages(int start, int end) {
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

  private TCGroupManagerImpl createTCGroupManager(Node node) throws Exception {
    StageManager stageManager = new StageManagerImpl(threadGroup, new QueueFactory());
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(), node.getPort(), node
        .getGroupPort(), stageManager, null);
    ConfigurationContext context = new ConfigurationContextImpl(stageManager);
    stageManager.startAll(context, Collections.emptyList());
    return gm;
  }

  private StateManager createStateManageNode(int localIndex, ChangeSink[] chgSinks,
                                             VirtualTCGroupManagerImpl[] virtualMgr) throws Exception {

    chgSinks[localIndex] = new ChangeSink(localIndex);
    MyStateManagerConfig config = new MyStateManagerConfig();
    config.electionTime = 5;
    StateManager mgr = new StateManagerImpl(logger, virtualMgr[localIndex], chgSinks[localIndex], config,
                                            WeightGeneratorFactory.createDefaultFactory(), new TestClusterStatePersistor());
    chgSinks[localIndex].setStateManager(mgr);
    return (mgr);
  }

  private static class L2StateMessageStage extends Thread {
    private final MockSink<L2StateMessage>               sink;
    private final NoExceptionLinkedQueue<L2StateMessage> processQ = new NoExceptionLinkedQueue<>();
    private final StateManager           mgr;
    private volatile boolean             stop     = false;

    public L2StateMessageStage(StateManager mgr) {
      this.mgr = mgr;
      this.sink = new MockSink<L2StateMessage>() {
        @Override
        public void addSingleThreaded(L2StateMessage ec) {
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

    public Sink<L2StateMessage> getSink() {
      return sink;
    }

    @Override
    public void run() {
      while (!isStopped()) {
        L2StateMessage m = processQ.poll(3000);
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

    @Override
    public void run() {
      mgr.startElection();
    }
  }

  private static class MyStateManagerConfig implements StateManagerConfig {
    public int electionTime;

    @Override
    public int getElectionTimeInSecs() {
      return electionTime;
    }
  }

  private static class ChangeSink extends MockSink<StateChangedEvent> {
    private final int         serverIndex;
    private StateChangedEvent event = null;
    private StateManager      stateManager;

    public ChangeSink(int index) {
      serverIndex = index;
    }

    public void setStateManager(StateManager mgr) {
      this.stateManager = mgr;
    }

    @Override
    public void addSingleThreaded(StateChangedEvent event) {
      this.event = event;
      System.out.println("*** Server[" + serverIndex + "]: " + event);
      stateManager.fireStateChangedEvent(event);
    }

    public State getState() {
      if (event == null) return null;
      return event.getCurrentState();
    }

    @Override
    public String toString() {
      State st = getState();
      return ((st != null) ? st.toString() : "<state unknown>");
    }

  }

  private static class MyGroupEventListener implements GroupEventsListener {

    private final NodeID gmNodeID;

    public MyGroupEventListener(NodeID nodeID) {
      this.gmNodeID = nodeID;
    }

    @Override
    public void nodeJoined(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeJoined -> " + nodeID);
    }

    @Override
    public void nodeLeft(NodeID nodeID) {
      System.err.println("\n### " + gmNodeID + ": nodeLeft -> " + nodeID);
    }

  }

  private static final class MyListener implements GroupMessageListener {

    NoExceptionLinkedQueue<GroupMessage> queue = new NoExceptionLinkedQueue<>();

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.put(msg);
    }

    public GroupMessage take() {
      return queue.take();
    }

    public boolean isEmpty() {
      return queue.isEmpty();
    }

  }

  private class MessagePackage {
    private final GroupMessage message;
    private final NodeID       nodeID;

    MessagePackage(NodeID nodeID, GroupMessage message) {
      this.message = message;
      this.nodeID = nodeID;
    }

    GroupMessage getMessage() {
      return this.message;
    }

    NodeID getNodeID() {
      return this.nodeID;
    }
  }

  private class TestGroupMessageListener implements GroupMessageListener {
    private final long                                timeout;
    private final LinkedBlockingQueue<MessagePackage> queue = new LinkedBlockingQueue<>(100);

    TestGroupMessageListener(long timeout) {
      this.timeout = timeout;
    }

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      queue.add(new MessagePackage(fromNode, msg));
    }

    public MessagePackage poll() throws InterruptedException {
      return (queue.poll(timeout, TimeUnit.MILLISECONDS));
    }

    public GroupMessage getNextMessageFrom(NodeID nodeID) throws InterruptedException {
      MessagePackage pkg = poll();
      assertNotNull("Failed to receive message from " + nodeID, pkg);
      assertEquals(groupIDServerIDMap.get(nodeID), pkg.getNodeID());
      return (pkg.getMessage());
    }
  }

  private class ResponseL2StateMessageListener extends TestGroupMessageListener {
    TCGroupManagerImpl manager;

    ResponseL2StateMessageListener(TCGroupManagerImpl manager, long timeout) {
      super(timeout);
      this.manager = manager;
    }

    @Override
    public void messageReceived(NodeID fromNode, GroupMessage msg) {
      super.messageReceived(fromNode, msg);
      L2StateMessage message = (L2StateMessage) msg;
      AbstractGroupMessage resultAgreed = L2StateMessage.createResultAgreedMessage(message, message.getEnrollment());
      try {
        manager.sendTo(message.messageFrom(), resultAgreed);
      } catch (GroupException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private L2StateMessage createL2StateMessage() {
    long weights[] = new long[] { 1, 23, 44, 78 };
    Enrollment enroll = new Enrollment(new ServerID("test", UUID.getUUID().toString().getBytes()), true, weights);
    L2StateMessage message = new L2StateMessage(L2StateMessage.START_ELECTION, enroll);
    return (message);
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

    @Override
    protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
      msg = in.readString();
    }

    @Override
    protected void basicSerializeTo(TCByteBufferOutput out) {
      out.writeString(msg);
    }

    String msg;

    @Override
    public int hashCode() {
      return msg.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof TestMessage) {
        TestMessage other = (TestMessage) o;
        return this.msg.equals(other.msg);
      }
      return false;
    }

    @Override
    public String toString() {
      return "TestMessage [ " + msg + "]";
    }
  }
}
