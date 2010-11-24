/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.operatorevent;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.ZapEventListener;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
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
    for (int i = 0; i < serverNames.length; i++) {
      try {
        L2DSOConfig l2Config = configSetupManager.dsoL2ConfigFor(serverNames[i]);
        this.nodeNameToServerName.put(l2Config.host() + ":" + l2Config.dsoPort().getIntValue(),
                                      serverNames[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
    }

  }

  public void fireBackOffEvent(NodeID winnerNode) {
    Assert.assertTrue(winnerNode instanceof ServerID);
    String remoteServerName = this.nodeNameToServerName.get(((ServerID) winnerNode).getName());

    Assert.assertNotNull(remoteServerName);
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
        .createZapRequestAcceptedEvent(new Object[] { winnerNode }));
  }

  public void fireSplitBrainEvent(NodeID node1, NodeID node2) {
    Assert.assertTrue(node1 instanceof ServerID);
    Assert.assertTrue(node2 instanceof ServerID);
    String localServerName = this.nodeNameToServerName.get(((ServerID) node1).getName());
    String remoteServerName = this.nodeNameToServerName.get(((ServerID) node2).getName());

    Assert.assertNotNull(localServerName);
    Assert.assertNotNull(remoteServerName);
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createZapRequestReceivedEvent(new Object[] {
        localServerName, remoteServerName }));
  }

}
