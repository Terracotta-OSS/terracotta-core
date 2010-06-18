/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.operatorevent;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.groups.GroupEventsListener;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.Map;

public class OperatorEventsNodeConnectionListener implements GroupEventsListener {
  
  private final Map<String, String>           nodeNameToServerName = new HashMap<String, String>();
  private final TerracottaOperatorEventLogger operatorEventLogger  = TerracottaOperatorEventLogging.getEventLogger();

  public OperatorEventsNodeConnectionListener(L2TVSConfigurationSetupManager configSetupManager) {
    initializeServerNameMap(configSetupManager);
  }

  private void initializeServerNameMap(L2TVSConfigurationSetupManager configSetupManager) {
    String[] serverNames = configSetupManager.allCurrentlyKnownServers();
    for (int i = 0; i < serverNames.length; i++) {
      try {
        NewL2DSOConfig l2Config = configSetupManager.dsoL2ConfigFor(serverNames[i]);
        this.nodeNameToServerName.put(l2Config.host().getString() + ":" + l2Config.listenPort().getInt(),
                                      serverNames[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
    }

  }

  public void nodeJoined(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    String serverName = this.nodeNameToServerName.get(((ServerID) nodeID).getName());
    Assert.assertNotNull(serverName);
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(serverName));
  }

  public void nodeLeft(NodeID nodeID) {
    Assert.assertTrue(nodeID instanceof ServerID);
    String serverName = this.nodeNameToServerName.get(((ServerID) nodeID).getName());
    Assert.assertNotNull(serverName);
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(serverName));
  }

}
