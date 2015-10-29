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
import com.tc.async.api.PostInit;
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
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManager;
import com.tc.l2.state.StateManagerConfig;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.proxy.TCPProxy;
import com.tc.object.net.groups.HaConfigForGroupNameTests;
import com.tc.objectserver.persistence.TestClusterStatePersistor;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.State;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;


public class ActiveServerIDStateManagerTest extends TCTestCase {

  private final static String     LOCALHOST          = "localhost";
  private static final TCLogger   logger             = TCLogging.getLogger(TCGroupManagerImpl.class);
  private TCThreadGroup           threadGroup;

  private TCGroupManagerImpl[]    groupMgrs          = null;
  private Node[]                  allNodes           = null;

  private Node[]                  proxiedAllNodes    = null;
  private TCPProxy[]              proxy              = null;

  private ChangeSink[]            sinks              = null;
  private StateManager[]          managers           = null;
  private ElectionThread[]        elections          = null;
  private L2StateMessageStage[]   msgStages          = null;
  private ActiveServerIDManager[] serverIDManagers   = null;
  private Map<GroupID, ServerID>  groupIDServerIDMap = null;
  private Map<GroupID, Set<Node>> groupNodesMap      = null;

  @Override
  public void setUp() {
    logger.setLevel(LogLevelImpl.DEBUG);
    threadGroup = new TCThreadGroup(new ThrowableHandlerImpl(logger), "TCGroupManagerNodeJoinedTest");
    TCLogging.getLogger(TCConnection.class).setLevel(LogLevelImpl.DEBUG);
    TCLogging.getLogger("com.tc.net.core.CoreNIOServices").setLevel(LogLevelImpl.DEBUG);
  }

  public void testStateManager4Servers2Groups() throws Exception {
    System.out.println("*** Testing 4 servers in 2 groups and join concurrently");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid1);
    nodesGroupMap.put(2, gid2);
    nodesGroupMap.put(3, gid2);

    nodesConcurrentJoining(4, nodesGroupMap);
  }

  /**
   * Test for DEV-4870 - problem 2
   */
  public void testStripeDisconnectAndReconnect() throws Exception {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "true");

    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid2);
    stripeNodesDisconnectAndReconnect(2, nodesGroupMap);

    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED, "false");
  }

  public void testStateManager8Servers3Groups() throws Exception {
    System.out.println("*** Testing 8 servers in 3 groups and join concurrently");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    GroupID gid3 = new GroupID(3);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid1);
    nodesGroupMap.put(2, gid2);
    nodesGroupMap.put(3, gid2);
    nodesGroupMap.put(4, gid3);
    nodesGroupMap.put(5, gid3);
    nodesGroupMap.put(6, gid3);
    nodesGroupMap.put(7, gid3);

    nodesConcurrentJoining(8, nodesGroupMap);
  }

  public void testStateManagerMixJoin6Servers2Groups() throws Exception {
    System.out.println("*** Testing MixJoin 6 servers in 2 groups and mix join ");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid1);
    nodesGroupMap.put(2, gid1);
    nodesGroupMap.put(3, gid2);
    nodesGroupMap.put(4, gid2);
    nodesGroupMap.put(5, gid2);

    nodesMixJoinAndElect(6, nodesGroupMap);
  }

  public void testStateManagerStopActives4Servers2Groups() throws Exception {
    System.out.println("*** Testing 4 servers in 2 groups and join, stop actives");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid1);
    nodesGroupMap.put(2, gid2);
    nodesGroupMap.put(3, gid2);

    nodesStopActives(4, nodesGroupMap);
  }

  public void testStateManagerStopActives8Servers2Groups() throws Exception {
    System.out.println("*** Testing 8 servers in 2 groups and join, stop actives");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid2);
    nodesGroupMap.put(2, gid1);
    nodesGroupMap.put(3, gid2);
    nodesGroupMap.put(4, gid1);
    nodesGroupMap.put(5, gid2);
    nodesGroupMap.put(6, gid1);
    nodesGroupMap.put(7, gid2);

    nodesStopActives(8, nodesGroupMap);
  }

  public void testStateManagerStopActives8Servers3Groups() throws Exception {
    System.out.println("*** Testing 8 servers in 3 groups and join, stop actives");
    Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
    GroupID gid1 = new GroupID(1);
    GroupID gid2 = new GroupID(2);
    GroupID gid3 = new GroupID(3);
    nodesGroupMap.put(0, gid1);
    nodesGroupMap.put(1, gid1);
    nodesGroupMap.put(2, gid2);
    nodesGroupMap.put(3, gid2);
    nodesGroupMap.put(4, gid3);
    nodesGroupMap.put(5, gid3);
    nodesGroupMap.put(6, gid3);
    nodesGroupMap.put(7, gid3);

    nodesStopActives(8, nodesGroupMap);
  }

  private void stripeNodesDisconnectAndReconnect(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    initiate(nodes, nodesGroupMap, true);
    // let Stripe 1 see Stripe 0, but not the other way
    proxy[1].stop();
    System.err.println("XXX Start Joining...");
    for (int i = 0; i < nodes; ++i) {
      Set<Node> nodeSet = new HashSet<>();
      Collections.addAll(nodeSet, proxiedAllNodes);
      NodesStore nodeStore = new NodesStoreImpl(nodeSet);
      groupMgrs[i].join(allNodes[i], nodeStore);
    }

    ThreadUtil.reallySleep(1000 * nodes * 2);

    System.err.println("XXX Start Election...");
    for (int i = 0; i < nodes; ++i) {
      elections[i].start();
    }

    System.err.println("XXX Join Election...");
    for (int i = 0; i < nodes; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes * 2);

    System.err.println("XXX Verify...");
    verify(nodes, nodesGroupMap);

    ThreadUtil.reallySleep(5000);

    System.err.println("XXX Stripe0 closes Stripe1 member");
    groupMgrs[0].closeMember((ServerID) groupMgrs[1].getLocalNodeID());

    System.err.println("XXX waiting for reconnects");
    ThreadUtil.reallySleep(TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT)
                           + TCPropertiesImpl.getProperties()
                               .getLong(TCPropertiesConsts.TC_TRANSPORT_HANDSHAKE_TIMEOUT));

    ThreadUtil.reallySleep(1000 * nodes * 5);
    System.err.println("XXX Verify again...");
    verify(nodes, nodesGroupMap);

    stopMessageStages();
    shutdown(groupMgrs);
  }

  private void nodesConcurrentJoining(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {

    initiate(nodes, nodesGroupMap);

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

    verify(nodes, nodesGroupMap);

    stopMessageStages();
    shutdown(groupMgrs);
  }

  private void nodesMixJoinAndElect(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    initiate(nodes, nodesGroupMap);

    // Joining and Electing
    System.out.println("*** Start Joining and Electing...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    groupMgrs[0].join(allNodes[0], nodeStore);
    for (int i = 0; i < nodes - 1; ++i) {
      elections[i].start();
      nodeSet = new HashSet<>();
      Collections.addAll(nodeSet, allNodes);
      nodeStore = new NodesStoreImpl(nodeSet);
      groupMgrs[i + 1].join(allNodes[i + 1], nodeStore);
    }
    elections[nodes - 1].start();

    for (int i = 0; i < nodes; ++i) {
      elections[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);

    verify(nodes, nodesGroupMap);

    stopMessageStages();
    shutdown(groupMgrs);

  }

  private void nodesStopActives(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {

    initiate(nodes, nodesGroupMap);

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

    verify(nodes, nodesGroupMap);

    System.out.println("*** Pretending to do passive sync...");
    for (int i = 0; i < nodes; i++) {
      if (!managers[i].isActiveCoordinator()) {
        managers[i].moveToPassiveStandbyState();
      }
    }

    System.out.println("*** Stopping actives...");
    for (int i = 0; i < nodes; ++i) {
      if (managers[i].isActiveCoordinator()) {
        System.out.println("*** Stop Server[" + i + "]");
        shutdown(groupMgrs, i, i + 1);
      }
    }

    ThreadUtil.reallySleep(2000);

    ElectionIfNecessaryThread reElectThreads[] = new ElectionIfNecessaryThread[nodes];
    for (int i = 0; i < nodes; ++i) {
      if (groupMgrs[i].isStopped()) continue;
      reElectThreads[i] = new ElectionIfNecessaryThread(managers[i], groupIDServerIDMap.get((nodesGroupMap
          .get(i))));
    }
    for (int i = 0; i < nodes; ++i) {
      if (groupMgrs[i].isStopped()) continue;
      reElectThreads[i].start();
    }
    for (int i = 0; i < nodes; ++i) {
      if (groupMgrs[i].isStopped()) continue;
      reElectThreads[i].join();
    }

    ThreadUtil.reallySleep(1000 * nodes);

    int activeCount = 0;
    groupIDServerIDMap = new HashMap<>();
    for (int i = 0; i < nodes; ++i) {
      if (groupMgrs[i].isStopped()) continue;
      if (managers[i].isActiveCoordinator()) {
        ++activeCount;
        groupIDServerIDMap.put(nodesGroupMap.get(i), (ServerID) groupMgrs[i].getLocalNodeID());
      }
      System.out.println("*** Server[" + i + "] state is " + sinks[i]);
    }
    assertEquals("Active coordinator", groupNodesMap.size(), activeCount);

    for (int i = 0; i < nodes; ++i) {
      if (groupMgrs[i].isStopped()) continue;
      ActiveServerIDManager serverIDManager = serverIDManagers[i];
      for (GroupID gid : groupIDServerIDMap.keySet()) {
        TestCase.assertEquals("Node[" + i + "] ", groupIDServerIDMap.get(gid), serverIDManager.getActiveServerIDFor(gid));
      }
    }

    stopMessageStages();
    shutdown(groupMgrs);
  }

  private void initiate(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    initiate(nodes, nodesGroupMap, false);
  }

  private void initiate(int nodes, Map<Integer, GroupID> nodesGroupMap, boolean withProxy) throws Exception {
    System.out.println("*** Testing total=" + nodes + " nodes join at same time.");

    groupMgrs = new TCGroupManagerImpl[nodes];
    allNodes = new Node[nodes];
    int[] ports = new int[nodes];
    PortChooser pc = new PortChooser();

    if (withProxy) {
      proxiedAllNodes = new Node[nodes];
      proxy = new TCPProxy[nodes];
    }

    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);

    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);

      if (withProxy) {
        int proxyPort = pc.chooseRandomPort();
        proxy[i] = new TCPProxy(proxyPort, InetAddress.getByName(LOCALHOST), ports[i] + 1, 0, false, null);
        proxy[i].start();
        proxiedAllNodes[i] = new Node(LOCALHOST, ports[i], proxyPort);
      }

      groupMgrs[i] = createTCGroupManager(allNodes[i]);
      groupMgrs[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupMgrs[i]));
    }

    groupNodesMap = new HashMap<>();
    for (int i = 0; i < nodes; ++i) {
      GroupID gid = nodesGroupMap.get(i);
      Set<Node> grpNodes = groupNodesMap.get(gid);
      if (grpNodes == null) {
        grpNodes = new HashSet<>();
        groupNodesMap.put(gid, grpNodes);
      }
      grpNodes.add(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgrs = new VirtualTCGroupManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      GroupID gid = nodesGroupMap.get(i);

      HashSet<String> names = new HashSet<>();
      for (Node node : groupNodesMap.get(gid)) {
        names.add(node.getServerNodeName());
      }

      virtualMgrs[i] = new VirtualTCGroupManagerImpl(groupMgrs[i],
                                                     new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    sinks = new ChangeSink[nodes];
    managers = new StateManager[nodes];
    elections = new ElectionThread[nodes];
    msgStages = new L2StateMessageStage[nodes];
    serverIDManagers = new ActiveServerIDManager[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = createStateManageNode(i, sinks, virtualMgrs, msgStages);
      elections[i] = new ElectionThread(managers[i]);
      serverIDManagers[i] = new ActiveServerIDManagerImpl(managers[i], groupMgrs[i], nodesGroupMap.get(i), nodeStore);
    }

  }

  private void verify(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {
    // verification
    int activeCount = 0;
    groupIDServerIDMap = new HashMap<>();
    for (int i = 0; i < nodes; ++i) {
      if (managers[i].isActiveCoordinator()) {
        ++activeCount;
        groupIDServerIDMap.put(nodesGroupMap.get(i), (ServerID) groupMgrs[i].getLocalNodeID());
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
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(), node.getPort(),
                                                   node.getGroupPort(), stageManager, null);
    ConfigurationContext context = new ConfigurationContextImpl(stageManager);
    stageManager.startAll(context, Collections.<PostInit>emptyList());
    return gm;
  }

  private StateManager createStateManageNode(int localIndex, ChangeSink[] chgSinks,
                                             VirtualTCGroupManagerImpl[] virtualMgr, L2StateMessageStage[] messageStages)
      throws Exception {
    VirtualTCGroupManagerImpl gm = virtualMgr[localIndex];

    MyGroupEventListener gel = new MyGroupEventListener(gm.getLocalNodeID());
    MyListener l = new MyListener();
    gm.registerForMessages(TestMessage.class, l);
    gm.registerForGroupEvents(gel);
    chgSinks[localIndex] = new ChangeSink(localIndex);
    MyStateManagerConfig config = new MyStateManagerConfig();
    config.electionTime = 5;
    StateManager mgr = new StateManagerImpl(logger, gm, chgSinks[localIndex], config,
        RandomWeightGenerator.createTestingFactory(2), new TestClusterStatePersistor());
    chgSinks[localIndex].setStateManager(mgr);
    messageStages[localIndex] = new L2StateMessageStage(mgr);
    gm.routeMessages(L2StateMessage.class, messageStages[localIndex].getSink());
    messageStages[localIndex].start();
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

  private static class ElectionIfNecessaryThread extends Thread {
    private final StateManager mgr;
    private final NodeID       disconnectedNode;

    public ElectionIfNecessaryThread(StateManager mgr, NodeID disconnectedNode) {
      this.mgr = mgr;
      this.disconnectedNode = disconnectedNode;
    }

    @Override
    public void run() {
      mgr.startElectionIfNecessary(disconnectedNode);
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
    public void addSingleThreaded(StateChangedEvent context) {
      event = context;
      System.out.println("*** State Change Sink: Server[" + serverIndex + "]: " + event);
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
