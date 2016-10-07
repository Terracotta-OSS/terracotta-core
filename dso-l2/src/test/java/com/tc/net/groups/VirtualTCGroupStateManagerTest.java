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

import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.msg.L2StateMessage;
import com.tc.l2.state.StateManagerImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.net.groups.HaConfigForGroupNameTests;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import java.util.Arrays;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;


public class VirtualTCGroupStateManagerTest extends TCTestCase {

  private final static String   LOCALHOST = "localhost";
  private static final TCLogger logger    = TCLogging.getLogger(VirtualTCGroupStateManagerTest.class);
  private TestThrowableHandler throwableHandler;
  private TCThreadGroup         threadGroup;
  private MockStageManagerFactory      stages;
  @Override
  public void setUp() {
    throwableHandler = new TestThrowableHandler(logger);
    threadGroup = new TCThreadGroup(throwableHandler, "VirtualTCGroupStateManagerTestGroup");
    stages = new MockStageManagerFactory(logger, new ThreadGroup(threadGroup, "state_managers"));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      throwableHandler.throwIfNecessary();
      stages.shutdown();
    } catch (Throwable throwable) {
      throw new Exception(throwable);
    }
  }

  public void testStateManagerTwoServers() throws Exception {
    // 2 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(4, 2);
  }

  public void testStateManagerThreeServers() throws Exception {
    // 3 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(6, 3);
  }

  public void testStateManagerSixServers() throws Exception {
    // 6 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(8, 6);
  }

  public void testStateManagerMixJoinAndElect3() throws Exception {
    // 3 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(6, 3);
  }

  public void testStateManagerMixJoinAndElect6() throws Exception {
    // 6 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(8, 6);
  }

  public void testStateManagerJoinLater3() throws Exception {
    // first node shall be active and remaining 2 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(6, 3);
  }

  public void testStateManagerJoinLater6() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(9, 6);
  }

  // -----------------------------------------------------------------------

  private void nodesConcurrentJoining(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at same time.");
// force gc to try and free uncollected ports
    System.gc();
    
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
      groupMgr[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupMgr[i], allNodes[i]));
    }

    // joining
    System.out.println("*** Start Joining...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i].join(allNodes[i], nodeStore);
    }

    System.out.println("*** Start Election...");
    // run them concurrently
    for (int i = 0; i < virtuals; ++i) {
      managers[i].startElection();
    }
    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState() + " node is " + allNodes[i] + " active node is " + managers[i].getActiveNodeID());
    }
    assertEquals("Active coordinator", 1, activeCount);

    shutdown(groupMgr);
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr) {
    // shut them down
    shutdown(groupMgr, 0, groupMgr.length);
  }

  private void shutdown(TCGroupManagerImpl[] groupMgr, int start, int end) {
    for (int i = start; i < end; ++i) {
      try {
        groupMgr[i].stop(1000);
      } catch (Exception ex) {
        logger.warn("*** Failed to stop Server[" + i + "] " + groupMgr[i], ex);
      }
    }
    System.out.println("*** shutdown done");
  }

  private void nodesMixJoinAndElect(int nodes, int virtuals) throws Exception {
    System.out.println("*** Testing total=" + nodes + " with " + virtuals
                       + " nodes mixed join and election at same time.");
// force gc to try and free uncollected ports
    System.gc();
    
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
      groupMgr[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupMgr[i], allNodes[i]));
    }

    // Joining and Electing
    System.out.println("*** Start Joining and Electing..." + Arrays.asList(allNodes));
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    groupMgr[0].join(allNodes[0], nodeStore);
    managers[0].startElection();
    managers[0].waitForDeclaredActive();
    Set<NodeID> nodeIDs = new HashSet<>();
    for(int i = 1; i < virtuals; i++) {
      nodeIDs.add(groupMgr[i].getLocalNodeID());
    }
    managers[0].addKnownServersList(nodeIDs);
    for (int i = 1; i < virtuals; ++i) {
      groupMgr[i].join(allNodes[i], nodeStore);
      managers[i].startElection();
      managers[i].waitForDeclaredActive();
      managers[i].moveToPassiveStandbyState();
    }

    // verification
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState() + " node is " + allNodes[i] + " active node is " + managers[i].getActiveNodeID());
    }
    assertEquals("Active coordinator", 1, activeCount);

    shutdown(groupMgr);
  }

  private void nodesJoinLater(int nodes, int virtuals) throws Exception {
// force gc to try and free uncollected ports
    System.gc();
    System.out.println("*** Testing total=" + nodes + " with " + virtuals + " nodes join at later time.");

    final LinkedBlockingQueue<NodeID> joinedNodes = new LinkedBlockingQueue<>();
    NodeID[] ids = new NodeID[nodes];
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgr[i] = createTCGroupManager(allNodes[i]);
    }

    VirtualTCGroupManagerImpl[] virtualMgr = new VirtualTCGroupManagerImpl[virtuals];
    Node[] virtualNodes = new Node[virtuals];
    HashSet<String> names = new HashSet<>();
    for (int i = 0; i < virtuals; ++i) {
      virtualNodes[i] = allNodes[i];
      names.add(virtualNodes[i].getServerNodeName());
    }
    for (int i = 0; i < virtuals; ++i) {
      virtualMgr[i] = new VirtualTCGroupManagerImpl(groupMgr[i], new HaConfigForGroupNameTests(names).getClusterInfo());
    }

    final StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
      groupMgr[i].setDiscover(new TCGroupMemberDiscoveryStatic(groupMgr[i], allNodes[i]));
    }

    // the first node to be the active one
    System.out.println("*** First node joins to be an active node...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    ids[0] = groupMgr[0].join(allNodes[0], nodeStore);
    managers[0].startElection();
    managers[0].waitForDeclaredActive();

    Set<NodeID> nodeIDs = new HashSet<>();
    for(int i = 1; i < virtuals; i++) {
      nodeIDs.add(groupMgr[i].getLocalNodeID());
    }

    managers[0].addKnownServersList(nodeIDs);
    
    for (int i=1;i<virtuals;i++) {
      managers[i].startElection();
    }
    // move following join nodes to passive-standby
    virtualMgr[0].registerForGroupEvents(new GroupEventsListener() {
      @Override
      public void nodeJoined(NodeID nodeID) {
        // save nodeID for moving to passive
        joinedNodes.add(nodeID);
      }

      @Override
      public void nodeLeft(NodeID nodeID) {

      }      
    });

    System.out.println("***  Remaining nodes join");
    nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    nodeStore = new NodesStoreImpl(nodeSet);
    for (int i = 1; i < nodes; ++i) {
      ids[i] = groupMgr[i].join(allNodes[i], nodeStore);
    }

    int nodesNeedToMoveToPassive = virtuals - 1;
    while (nodesNeedToMoveToPassive > 0) {
      NodeID toBePassiveNode = joinedNodes.take();
      System.out.println("*** moveNodeToPassiveStandby -> " + toBePassiveNode);
      for (int i=0;i<virtuals;i++) {
        if (ids[i].equals(toBePassiveNode)) {
          managers[i].waitForDeclaredActive();
          managers[i].moveToPassiveStandbyState();
          --nodesNeedToMoveToPassive;
        }
      }
    }
    assertTrue(nodesNeedToMoveToPassive == 0);

    // verification: first node must be active
    int activeCount = 0;
    for (int i = 0; i < virtuals; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState());
    }
    assertEquals("Active coordinator", 1, activeCount);
    assertTrue("Node-0 must be active coordinator", managers[0].isActiveCoordinator());

    System.out.println("*** Stop active and re-elect");
    // stop active node
    shutdown(groupMgr, 0, 1);
    managers[0].endElection();

    for (int i = 1; i < virtuals; ++i) {
      managers[i].startElectionIfNecessary(ids[0]);
    }

    // verify
    activeCount = 0;
    for (int i = 1; i < virtuals; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] (" + (active ? "active" : "non-active") + ")state is " + managers[i].getCurrentState());
    }
    if (activeCount != 1) {
      System.out.println("fail");
    }
    assertEquals("Active coordinator", 1, activeCount);

    // shut them down
    shutdown(groupMgr, 1, nodes);
  }

  private TCGroupManagerImpl createTCGroupManager(Node node) throws Exception {
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(), node.getPort(), node.getGroupPort(), stages.createStageManager(), null, RandomWeightGenerator.createTestingFactory(2));
    return gm;
  }
}
