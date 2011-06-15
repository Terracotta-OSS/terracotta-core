/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

public class ReconnectionRejectedListenerImpl implements ChannelEventListener {
  private static final TCLogger             DSO_LOGGER     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger             CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();
  private final DsoClusterInternalEventsGun dsoClusterEventsGun;
  private final ClientHandshakeManager      clientHandshakeManager;

  public ReconnectionRejectedListenerImpl(final DsoClusterInternalEventsGun dsoClusterEventsGun,
                                          ClientHandshakeManager clientHandshakeManager) {
    this.dsoClusterEventsGun = dsoClusterEventsGun;
    this.clientHandshakeManager = clientHandshakeManager;
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if ((event.getType() == ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT)
        || (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT)) {
      ChannelID channelID = event.getChannelID();
      String msg = (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT) ? "Channel " + channelID + " closed."
          : "Reconnection rejected event fired, caused by " + channelID;
      CONSOLE_LOGGER.info(msg);
      DSO_LOGGER.info(msg);
      // fire event first
      this.dsoClusterEventsGun.fireThisNodeLeft();
      DSO_LOGGER.info("Shutting down clientHandshakeManager...");
      clientHandshakeManager.shutdown();
    }
  }
}
