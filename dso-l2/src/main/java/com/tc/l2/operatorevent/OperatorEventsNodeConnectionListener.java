/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.operatorevent;

import com.tc.config.NodesStore;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.util.Assert;

public class OperatorEventsNodeConnectionListener implements GroupEventsListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
  private final NodesStore                    nodesStore;

  public OperatorEventsNodeConnectionListener(NodesStore nodesStore) {
    this.nodesStore = nodesStore;
  }

  @Override
  public void nodeJoined(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    String serverName = nodesStore.getServerNameFromNodeName(((ServerID) nodeID).getName());
    Assert.assertNotNull(serverName);
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.NODE_JOINED");
    tsaManagementEventPayload.getAttributes().put("Server.Name", serverName);
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(serverName));
  }

  @Override
  public void nodeLeft(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    String serverName = nodesStore.getServerNameFromNodeName(((ServerID) nodeID).getName());
    Assert.assertNotNull(serverName);
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.NODE_LEFT");
    tsaManagementEventPayload.getAttributes().put("Server.Name", serverName);
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(serverName));
  }

}
