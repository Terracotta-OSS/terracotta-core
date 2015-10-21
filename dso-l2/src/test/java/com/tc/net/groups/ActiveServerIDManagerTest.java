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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.tc.config.NodesStore;
import com.tc.config.NodesStoreImpl;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.ActiveJoinMessage;
import com.tc.l2.state.StateManager;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.TestActiveGroupManager.SendToMessage;
import com.tc.object.net.groups.HaConfigForGroupNameTests;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEventCallback;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

public class ActiveServerIDManagerTest extends TCTestCase {

  private final static String         LOCALHOST        = "localhost";

  private TestActiveGroupManager[]    groupMgrs        = null;
  private Node[]                      allNodes         = null;
  private TestStateManager[]          managers         = null;
  private TestActiveServerListener[]  asListeners      = null;
  private ActiveServerIDManagerImpl[] serverIDManagers = null;
  private Map<GroupID, Set<Node>>     groupNodesMap    = null;

  public ActiveServerIDManagerTest() {
    // disableAllUntil("2009-01-14");
  }

  public void testMessageReceived() throws Exception {
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

      managers[0].setActive(true);
      managers[2].setActive(true);
      managers[4].setActive(true);

      // ACTIVE_JOIN messages
      NodeID fromNode1 = groupMgrs[0].getLocalNodeID();
      GroupMessage msg1 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode1);
      NodeID fromNode2 = groupMgrs[2].getLocalNodeID();
      GroupMessage msg2 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode2);
      NodeID fromNode3 = groupMgrs[4].getLocalNodeID();
      GroupMessage msg3 = ActiveJoinMessage.createActiveJoinMessage(gid3, (ServerID) fromNode3);
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode1, msg1);
        groupMgrs[i].messageReceived(fromNode2, msg2);
        groupMgrs[i].messageReceived(fromNode3, msg3);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(3, serverIDManagers[i].getAllActiveServerIDs().size());
      }

      // duplicate join
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode2, msg2);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(3, serverIDManagers[i].getAllActiveServerIDs().size());
      }

      // one node left
      GroupMessage msg = ActiveJoinMessage.createActiveLeftMessage(gid3);
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode3, msg);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
      }

      // clean up sendTo
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].getSendToMessages().clear();
      }

      // request to join
      msg = ActiveJoinMessage.createActiveRequestJoinMessage(gid3, (ServerID) fromNode3);
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode3, msg);
      }
      // verify join messages sendTo active nodes
      for (int i = 0; i < nodes; ++i) {
        List<SendToMessage> sendTo = groupMgrs[i].getSendToMessages();
        if (managers[i].isActiveCoordinator() && nodesGroupMap.get(new Integer(i)).equals(gid3)) {
          Assert.assertEquals(1, sendTo.size());
          Assert.assertEquals(fromNode3, sendTo.get(0).getNodeID());
          Assert.assertEquals(ActiveJoinMessage.ACTIVE_JOIN, sendTo.get(0).getGroupMessage().getType());
          sendTo.clear();
        } else {
          Assert.assertEquals(0, sendTo.size());
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void testGroupSplitBrain() throws Exception {
    try {

      Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
      GroupID gid1 = new GroupID(1);
      GroupID gid2 = new GroupID(2);
      nodesGroupMap.put(new Integer(0), gid1);
      nodesGroupMap.put(new Integer(1), gid1);
      nodesGroupMap.put(new Integer(2), gid2);
      nodesGroupMap.put(new Integer(3), gid2);
      nodesGroupMap.put(new Integer(4), gid1);
      nodesGroupMap.put(new Integer(5), gid1);

      int nodes = 6;
      initiate(nodes, nodesGroupMap);

      managers[0].setActive(true);
      managers[2].setActive(true);
      managers[4].setActive(true);
      managers[5].setActive(true);

      // ACTIVE_JOIN messages
      NodeID fromNode1 = groupMgrs[0].getLocalNodeID();
      GroupMessage msg1 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode1);
      NodeID fromNode2 = groupMgrs[2].getLocalNodeID();
      GroupMessage msg2 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode2);

      // split brain at group 1
      NodeID fromNode3 = groupMgrs[4].getLocalNodeID();
      GroupMessage msg3 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode3);
      // another split brain at group 1
      NodeID fromNode4 = groupMgrs[5].getLocalNodeID();
      GroupMessage msg4 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode4);

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode1, msg1);
        groupMgrs[i].messageReceived(fromNode2, msg2);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
      }

      // two split brain servers join group 1
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode3, msg3);
        groupMgrs[i].messageReceived(fromNode4, msg4);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        if (serverIDManagers[i].getLocalGroupID().equals(gid1)) {
          Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
          Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
        } else {
          Assert.assertEquals(1, serverIDManagers[i].getIgnoredJoinsSize());
          Assert.assertEquals(2, serverIDManagers[i].getBlackListedServersSize());
        }
      }

      // group 1 active left, disconnect blacklisted nodes for other active to re-join
      msg1 = ActiveJoinMessage.createActiveLeftMessage(gid1);
      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode1, msg1);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(1, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void testActiveJoinMessageFromSameGroup() throws Exception {
    try {

      Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
      GroupID gid1 = new GroupID(1);
      GroupID gid2 = new GroupID(2);
      nodesGroupMap.put(new Integer(0), gid1);
      nodesGroupMap.put(new Integer(1), gid1);
      nodesGroupMap.put(new Integer(2), gid2);
      nodesGroupMap.put(new Integer(3), gid2);
      nodesGroupMap.put(new Integer(4), gid1);
      nodesGroupMap.put(new Integer(5), gid1);

      int nodes = 6;
      initiate(nodes, nodesGroupMap);

      managers[0].setActive(true);
      managers[2].setActive(true);
      managers[4].setActive(true);
      managers[5].setActive(true);

      // ACTIVE_JOIN messages
      NodeID fromNode1 = groupMgrs[0].getLocalNodeID();
      GroupMessage msg1 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode1);
      NodeID fromNode2 = groupMgrs[2].getLocalNodeID();
      GroupMessage msg2 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode2);

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode1, msg1);
        groupMgrs[i].messageReceived(fromNode2, msg2);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
      }

      // reject join from same group
      NodeID fromNode3 = groupMgrs[1].getLocalNodeID();
      GroupMessage msg3 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode3);
      NodeID fromNode4 = groupMgrs[3].getLocalNodeID();
      GroupMessage msg4 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode4);

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode3, msg3);
        groupMgrs[i].messageReceived(fromNode4, msg4);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(1, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(1, serverIDManagers[i].getBlackListedServersSize());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void testActiveJoinMessageDisconnectedNode() throws Exception {
    try {

      Map<Integer, GroupID> nodesGroupMap = new HashMap<>();
      GroupID gid1 = new GroupID(1);
      GroupID gid2 = new GroupID(2);
      nodesGroupMap.put(new Integer(0), gid1);
      nodesGroupMap.put(new Integer(1), gid1);
      nodesGroupMap.put(new Integer(2), gid2);
      nodesGroupMap.put(new Integer(3), gid2);
      nodesGroupMap.put(new Integer(4), gid1);
      nodesGroupMap.put(new Integer(5), gid1);

      int nodes = 6;
      initiate(nodes, nodesGroupMap);

      managers[0].setActive(true);
      managers[2].setActive(true);
      managers[4].setActive(true);
      managers[5].setActive(true);

      // ACTIVE_JOIN messages
      NodeID fromNode1 = groupMgrs[0].getLocalNodeID();
      GroupMessage msg1 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode1);
      NodeID fromNode2 = groupMgrs[2].getLocalNodeID();
      GroupMessage msg2 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode2);

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode1, msg1);
        groupMgrs[i].messageReceived(fromNode2, msg2);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
      }

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].setConnected(false);
      }
      // update mapping of disconnected node
      NodeID fromNode3 = groupMgrs[1].getLocalNodeID();
      GroupMessage msg3 = ActiveJoinMessage.createActiveJoinMessage(gid1, (ServerID) fromNode3);
      NodeID fromNode4 = groupMgrs[3].getLocalNodeID();
      GroupMessage msg4 = ActiveJoinMessage.createActiveJoinMessage(gid2, (ServerID) fromNode4);

      for (int i = 0; i < nodes; ++i) {
        groupMgrs[i].messageReceived(fromNode3, msg3);
        groupMgrs[i].messageReceived(fromNode4, msg4);
      }
      // verify
      for (int i = 0; i < nodes; ++i) {
        Assert.assertEquals(2, serverIDManagers[i].getAllActiveServerIDs().size());
        Assert.assertEquals(0, serverIDManagers[i].getIgnoredJoinsSize());
        Assert.assertEquals(0, serverIDManagers[i].getBlackListedServersSize());
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void testStateChanged() throws Exception {
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

      managers[0].setActive(true);
      managers[0].startElection();
      // verify sendAll messages in GroupManager
      List<SendToMessage> sendAll = groupMgrs[0].getSendAllMessages();
      Assert.assertEquals(1, sendAll.size());
      Assert.assertEquals(ActiveJoinMessage.ACTIVE_JOIN, sendAll.get(0).getGroupMessage().getType());

      managers[3].setActive(true);
      managers[3].startElection();
      // verify sendAll messages in GroupManager
      sendAll = groupMgrs[3].getSendAllMessages();
      Assert.assertEquals(1, sendAll.size());
      Assert.assertEquals(ActiveJoinMessage.ACTIVE_JOIN, sendAll.get(0).getGroupMessage().getType());
      sendAll.clear();

      // this is not possible but to exercise code path
      StateChangedEvent sce = new StateChangedEvent(StateManager.ACTIVE_COORDINATOR, StateManager.PASSIVE_STANDBY);
      managers[3].fireStateChangedEvent(sce);
      Assert.assertEquals(1, sendAll.size());
      Assert.assertEquals(ActiveJoinMessage.ACTIVE_LEFT, sendAll.get(0).getGroupMessage().getType());
      sendAll.clear();

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void testActiveL2DisconnectedOperatorEvent() throws Exception {
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

      managers[0].setActive(true);
      managers[0].startElection();

      managers[2].setActive(true);
      managers[2].startElection();

      managers[4].setActive(true);
      managers[4].startElection();

      TestTerracottaOperatorEventCallback operatorEventCallback = new TestTerracottaOperatorEventCallback();

      final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
      operatorEventLogger.registerEventCallback(operatorEventCallback);

      // this is not possible but to exercise code path
      StateChangedEvent sce = new StateChangedEvent(StateManager.ACTIVE_COORDINATOR, StateManager.PASSIVE_STANDBY);
      managers[0].fireStateChangedEvent(sce);

      assertThat(operatorEventCallback.events.size(), is(1));

      TerracottaOperatorEvent event = operatorEventCallback.events.get(0);

      assertThat(event.getEventLevel(), is(EventLevel.WARN));
      assertThat(event.getEventSubsystem(), is(EventSubsystem.CLUSTER_TOPOLOGY));
      assertThat(event.getEventMessage(), is("Active server " + ((ServerID) managers[0].getActiveNodeID()).getName()
                                             + " left the cluster"));

      System.out.println(operatorEventCallback.events);

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }


  private void initiate(int nodes, Map<Integer, GroupID> nodesGroupMap) throws Exception {

    groupMgrs = new TestActiveGroupManager[nodes];
    allNodes = new Node[nodes];
    Set<Node> nodeSet = new HashSet<>();
    Collections.addAll(nodeSet, allNodes);
    NodesStore nodeStore = new NodesStoreImpl(nodeSet);
    int[] ports = new int[nodes];
    PortChooser pc = new PortChooser();
    for (int i = 0; i < nodes; ++i) {
      ports[i] = pc.chooseRandom2Port();
      allNodes[i] = new Node(LOCALHOST, ports[i], ports[i] + 1);
      groupMgrs[i] = new TestActiveGroupManager(LOCALHOST, ports[i]);
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

      virtualMgrs[i] = new VirtualTCGroupManagerImpl(groupMgrs[i], new HaConfigForGroupNameTests(names)
          .getClusterInfo());
    }

    managers = new TestStateManager[nodes];
    asListeners = new TestActiveServerListener[nodes];
    serverIDManagers = new ActiveServerIDManagerImpl[nodes];
    for (int i = 0; i < nodes; ++i) {
      managers[i] = new TestStateManager(groupMgrs[i].getLocalNodeID());
      serverIDManagers[i] = new ActiveServerIDManagerImpl(managers[i], groupMgrs[i], nodesGroupMap.get(new Integer(i)),
                                                          nodeStore);
      asListeners[i] = new TestActiveServerListener(serverIDManagers[i]);
      serverIDManagers[i].addActiveServerListener(asListeners[i]);
    }
  }

  private class TestActiveServerListener implements ActiveServerListener {
    private final ActiveServerIDManagerImpl activeServerIDManager;

    public TestActiveServerListener(ActiveServerIDManagerImpl activeServerIDManager) {
      this.activeServerIDManager = activeServerIDManager;
    }

    @Override
    public void activeServerJoined(GroupID groupID, ServerID serverID) {
      this.activeServerIDManager.nodeJoined(serverID);
    }

    @Override
    public void activeServerLeft(GroupID groupID, ServerID serverID) {
      this.activeServerIDManager.nodeLeft(serverID);
    }

  }

  private class TestTerracottaOperatorEventCallback implements TerracottaOperatorEventCallback {
    private final List<TerracottaOperatorEvent> events = new ArrayList<>();

    @Override
    public void logOperatorEvent(TerracottaOperatorEvent event) {
      this.events.add(event);
    }

  }

}
