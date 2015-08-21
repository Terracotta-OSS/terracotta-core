/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.operatorevent;

import com.tc.config.NodesStore;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.ServerID;
import com.tc.net.groups.PassiveServerListener;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class OperatorEventsPassiveServerConnectionListener implements PassiveServerListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
  private final NodesStore                    nodesStore;

  public OperatorEventsPassiveServerConnectionListener(NodesStore nodesStore) {
    this.nodesStore = nodesStore;
  }


  @Override
  public void passiveServerJoined(ServerID nodeID) {
    // no-op
  }


  @Override
  public void passiveServerLeft(ServerID nodeID) {
    String serverName = nodesStore.getServerNameFromNodeName(nodeID.getName());
    if (serverName == null) {
      serverName = nodeID.getName();
    }
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.MIRROR_LEFT");
    tsaManagementEventPayload.getAttributes().put("Server.Name", serverName);
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());

    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createPassiveL2DisconnectedEvent(serverName));
  }

}
