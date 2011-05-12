/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class ClientChannelOperatorEventlistener implements DSOChannelManagerEventListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public void channelCreated(MessageChannel channel) {
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeConnectedEvent(channel
        .getRemoteNodeID().toString()));
  }

  public void channelRemoved(MessageChannel channel) {
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createNodeDisconnectedEvent(channel
        .getRemoteNodeID().toString()));
  }

}
