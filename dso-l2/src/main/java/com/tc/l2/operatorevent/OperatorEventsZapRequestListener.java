/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.operatorevent;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.ZapEventListener;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class OperatorEventsZapRequestListener implements ZapEventListener {

  private final Map<String, String>           nodeNameToServerName = new HashMap<String, String>();
  private final TerracottaOperatorEventLogger operatorEventLogger  = TerracottaOperatorEventLogging.getEventLogger();

  public OperatorEventsZapRequestListener(L2ConfigurationSetupManager configSetupManager) {
    initializeServerNameMap(configSetupManager);
  }

  private void initializeServerNameMap(L2ConfigurationSetupManager configSetupManager) {
    String[] serverNames = configSetupManager.allCurrentlyKnownServers();
    for (String serverName : serverNames) {
      try {
        L2DSOConfig l2Config = configSetupManager.dsoL2ConfigFor(serverName);
        this.nodeNameToServerName.put(l2Config.host() + ":" + l2Config.tsaPort().getIntValue(), serverName);
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
