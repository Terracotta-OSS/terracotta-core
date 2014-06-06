/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.management.TCManagementEvent;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class ClientChannelOperatorEventlistener implements DSOChannelManagerEventListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  @Override
  public void channelCreated(MessageChannel channel) {
    // Don't generate operator events for internal products
    if (!channel.getProductId().isInternal()) {
      NodeID remoteNodeID = channel.getRemoteNodeID();
      ClientID clientID = (ClientID)remoteNodeID;
      TerracottaRemoteManagement.getRemoteManagementInstance()
          .sendEvent(new TCManagementEvent(new TSAManagementEventPayload(Long.toString(clientID.toLong())), "TSA.TOPOLOGY.L1.CONNECTED"));
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(remoteNodeID.toString()));
    }
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    // Don't generate operator events for internal products
    if (!channel.getProductId().isInternal()) {
      NodeID remoteNodeID = channel.getRemoteNodeID();
      ClientID clientID = (ClientID)remoteNodeID;
      TerracottaRemoteManagement.getRemoteManagementInstance()
          .sendEvent(new TCManagementEvent(new TSAManagementEventPayload(Long.toString(clientID.toLong())), "TSA.TOPOLOGY.L1.DISCONNECTED"));
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(remoteNodeID.toString()));
    }
  }

}
