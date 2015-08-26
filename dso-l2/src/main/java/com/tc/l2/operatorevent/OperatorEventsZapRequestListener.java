/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.operatorevent;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.ZapEventListener;
import com.tc.object.config.schema.L2Config;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class OperatorEventsZapRequestListener implements ZapEventListener {

  private final Map<String, String>           nodeNameToServerName = new HashMap<>();
  private final TerracottaOperatorEventLogger operatorEventLogger  = TerracottaOperatorEventLogging.getEventLogger();

  public OperatorEventsZapRequestListener(L2ConfigurationSetupManager configSetupManager) {
    initializeServerNameMap(configSetupManager);
  }

  private void initializeServerNameMap(L2ConfigurationSetupManager configSetupManager) {
    String[] serverNames = configSetupManager.allCurrentlyKnownServers();
    for (String serverName : serverNames) {
      try {
        L2Config l2Config = configSetupManager.dsoL2ConfigFor(serverName);
        this.nodeNameToServerName.put(l2Config.host() + ":" + l2Config.tsaPort().getValue(), serverName);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
    }

  }

  @Override
  public void fireBackOffEvent(NodeID winnerNode) {
    Assert.assertTrue(winnerNode instanceof ServerID);
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.NODE_ZAPPED");
    tsaManagementEventPayload.getAttributes().put("Winner.Server.Name", ((ServerID)winnerNode).getName());
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
        .createZapRequestAcceptedEvent(new Object[] { winnerNode }));
  }

  @Override
  public void fireSplitBrainEvent(NodeID node1, NodeID node2) {
    Assert.assertTrue(node1 instanceof ServerID);
    Assert.assertTrue(node2 instanceof ServerID);
    String localServerName = this.nodeNameToServerName.get(((ServerID) node1).getName());
    String remoteServerName = this.nodeNameToServerName.get(((ServerID) node2).getName());

    localServerName = localServerName == null ? "NOT_FOUND" : localServerName;
    remoteServerName = remoteServerName == null ? "NOT_FOUND" : remoteServerName;

    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.SPLIT_BRAIN");
    tsaManagementEventPayload.getAttributes().put("Local.Server.Name", localServerName);
    tsaManagementEventPayload.getAttributes().put("Remote.Server.Name", remoteServerName);
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createZapRequestReceivedEvent(new Object[] {
        localServerName, remoteServerName }));
  }

}