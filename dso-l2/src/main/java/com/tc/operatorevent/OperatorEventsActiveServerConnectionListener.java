/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.config.NodesStore;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.groups.ActiveServerListener;

public class OperatorEventsActiveServerConnectionListener implements ActiveServerListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
  private final NodesStore                    nodesStore;

  public OperatorEventsActiveServerConnectionListener(NodesStore nodesStore) {
    this.nodesStore = nodesStore;
  }


  @Override
  public void activeServerJoined(GroupID groupID, ServerID serverID) {
    // no-op
  }

  @Override
  public void activeServerLeft(GroupID groupID, ServerID serverID) {
    String serverName = nodesStore.getServerNameFromNodeName(serverID.getName());
    if (serverName == null) {
      serverName = serverID.getName();
    }
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createActiveL2DisconnectedEvent(serverName));
  }

}
