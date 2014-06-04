/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.management.TSAManagementEvent;
import com.tc.management.TerracottaRemoteManagement;
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
      TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(
          new TSAManagementEvent("L1.CONNECTED", channel.getRemoteNodeID().toString()));
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(channel
          .getRemoteNodeID().toString()));
    }
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    // Don't generate operator events for internal products
    if (!channel.getProductId().isInternal()) {
      TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(
          new TSAManagementEvent("L1.DISCONNECTED", channel.getRemoteNodeID().toString()));
      operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(channel
          .getRemoteNodeID().toString()));
    }
  }

}
