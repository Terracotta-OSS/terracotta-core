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
import com.tcclient.cluster.DsoClusterInternal;

public class ReconnectionRejectedListenerImpl implements ChannelEventListener {
  private static final TCLogger    DSO_LOGGER     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger    CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();

  private final DsoClusterInternal dsoCluster;

  public ReconnectionRejectedListenerImpl(final DsoClusterInternal dsoCluster) {
    this.dsoCluster = dsoCluster;
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getType() == ChannelEventType.TRANSPORT_RECONNECTION_REJECTED_EVENT) {
      ChannelID channelID = event.getChannelID();
      String msg = "Reconnection rejceted event fired, caused by " + channelID;
      CONSOLE_LOGGER.info(msg);
      DSO_LOGGER.info(msg);
      this.dsoCluster.fireThisNodeLeft();
    }
  }

}
