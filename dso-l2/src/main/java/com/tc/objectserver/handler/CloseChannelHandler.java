/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.groups.L1RemovedGroupMessage;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.MessageChannel;

public class CloseChannelHandler extends AbstractEventHandler<L1RemovedGroupMessage> {
  private static final TCLogger logger = TCLogging.getLogger(CloseChannelHandler.class);
  private final ChannelManager  channelManager;

  public CloseChannelHandler(ChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public void handleEvent(L1RemovedGroupMessage msg) {
    final ClientID cid = msg.getClientID();
    MessageChannel channel = channelManager.getChannel(cid.getChannelID());
    if (channel != null) {
      logger.warn("Close " + cid + " due to disconnect from stripe " + msg.getGroupID());
      channel.close();
    }
  }

}
