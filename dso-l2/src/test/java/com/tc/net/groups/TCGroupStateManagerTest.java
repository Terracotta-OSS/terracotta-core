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

import com.google.common.base.Throwables;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.async.impl.StageManagerImpl;
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
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.QueueFactory;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.io.FileUtils;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mock;


public class TCGroupStateManagerTest extends TCTestCase {

  private final static String   LOCALHOST = "localhost";
  private static final TCLogger logger    = TCLogging.getLogger(StateManagerImpl.class);
  private TCThreadGroup         threadGroup;
  private TestThrowableHandler throwableHandler;
  private MockStageManagerFactory stages;

  @Override
  public void setUp() {
    throwableHandler = new TestThrowableHandler(logger);
    threadGroup = new TCThreadGroup(throwableHandler, "StateManagerTestGroup");
    stages = new MockStageManagerFactory(logger, new ThreadGroup(threadGroup, "state-managers"));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      throwableHandler.throwIfNecessary();
      stages.shutdown();
    } catch (Throwable throwable) {
      throw Throwables.propagate(throwable);
    }
  }

  public void testStateManagerTwoServers() throws Exception {
    // 2 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(2);
  }

  public void testStateManagerThreeServers() throws Exception {
    // 3 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(3);
  }

  public void testStateManagerSixServers() throws Exception {
    // 6 nodes join concurrently
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesConcurrentJoining(6);
  }

  public void testStateManagerMixJoinAndElect3() throws Exception {
    // 3 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(3);
  }

  public void testStateManagerMixJoinAndElect6() throws Exception {
    // 6 nodes mix join and election
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesMixJoinAndElect(6);
  }

  public void testStateManagerJoinLater3() throws Exception {
    // first node shall be active and remaining 2 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(3);
  }

  public void testStateManagerJoinLater6() throws Exception {
    // first node shall be active and remaining 5 nodes join later
    // setup throwable ThreadGroup to catch AssertError from threads.
    nodesJoinLater(6);
  }

  // -----------------------------------------------------------------------

  private void nodesConcurrentJoining(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes join at same time.");

    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
    }

    StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i] = createGroupManager(allNodes[i]);
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
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
    for (int i = 0; i < nodes; ++i) {
      managers[i].startElection();
    }
    for (int i = 0; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
    }
    // verification
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState());
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
        System.out.println("*** Failed to stop Server[" + i + "] " + groupMgr[i] + " " + ex);
      }
    }
    System.out.println("*** shutdown done");
  }

  private void nodesMixJoinAndElect(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes mixed join and election at same time.");

    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
    }

    StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      Sink incoming = mock(Sink.class);
      groupMgr[i] = createGroupManager(allNodes[i]);
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
    }

    // Joining and Electing
    System.out.println("*** Start Joining and Electing...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    groupMgr[0].join(allNodes[0], nodeStore);
    for (int i = 0; i < nodes - 1; ++i) {
      managers[i].startElection();
      groupMgr[i + 1].join(allNodes[i + 1], nodeStore);
    }
    managers[nodes - 1].startElection();

    managers[nodes - 1].waitForDeclaredActive();
    for (int i = 0; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
    }

    // verification
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState());
    }
    assertEquals("Active coordinator", 1, activeCount);

    shutdown(groupMgr);
  }

  private void nodesJoinLater(int nodes) throws Exception {
    System.out.println("*** Testing " + nodes + " nodes join at later time.");

    final LinkedBlockingQueue<NodeID> joinedNodes = new LinkedBlockingQueue<>();
    NodeID[] ids = new NodeID[nodes];
    TCGroupManagerImpl[] groupMgr = new TCGroupManagerImpl[nodes];
    PortChooser pc = new PortChooser();
    int[] ports = new int[nodes];
    Node[] allNodes = new Node[nodes];
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
    }

    final StateManagerImpl[] managers = new StateManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      groupMgr[i] = createGroupManager(allNodes[i]);
      TestStateManagerFactory state = new TestStateManagerFactory(stages, groupMgr[i], logger);
      managers[i] = state.getStateManager();
      groupMgr[i].routeMessages(L2StateMessage.class, state.getStateMessageSink());
    }

    // the first node to be the active one
    System.out.println("*** First node joins to be an active node...");
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    ids[0] = groupMgr[0].join(allNodes[0], nodeStore);
    managers[0].startElection();
    managers[0].waitForDeclaredActive();

    // move following join nodes to passive-standby
    groupMgr[0].registerForGroupEvents(new GroupEventsListener() {
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
      managers[i].startElection();
      ids[i] = groupMgr[i].join(allNodes[i], nodeStore);
    }
    
    for (int i = 1; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
    }

    int nodesNeedToMoveToPassive = nodes - 1;
    while (nodesNeedToMoveToPassive > 0) {
      NodeID toBePassiveNode = joinedNodes.take();
      System.out.println("*** moveNodeToPassiveStandby -> " + toBePassiveNode);
      managers[0].moveNodeToPassiveStandby(toBePassiveNode);
      --nodesNeedToMoveToPassive;
    }
    assertTrue(nodesNeedToMoveToPassive == 0);
    // verification: first node must be active
    int activeCount = 0;
    for (int i = 0; i < nodes; ++i) {
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] state is " + managers[i].getCurrentState());
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
    shutdown(groupMgr, 0, 1);

    for (int i = 1; i < nodes; ++i) {
      managers[i].startElectionIfNecessary(ids[0]);
    }

    // verify
    activeCount = 0;
    for (int i = 1; i < nodes; ++i) {
      managers[i].waitForDeclaredActive();
      boolean active = managers[i].isActiveCoordinator();
      if (active) ++activeCount;
      System.out.println("*** Server[" + i + "] (" + (active ? "active" : "non-active") + ")state is " + managers[i].getCurrentState());
    }
    assertEquals("Active coordinator", 1, activeCount);

    // shut them down
    shutdown(groupMgr, 1, nodes);
  }
  
  private TCGroupManagerImpl createGroupManager(Node node) throws Exception {
    TCGroupManagerImpl gm = new TCGroupManagerImpl(new NullConnectionPolicy(), node.getHost(),
                                                   node.getPort(), node.getGroupPort(),
                                                   stages.createStageManager(), null, RandomWeightGenerator.createTestingFactory(2));
    gm.setDiscover(new TCGroupMemberDiscoveryStatic(gm));

    return gm;
  }
}
