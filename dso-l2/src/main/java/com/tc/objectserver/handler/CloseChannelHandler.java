/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.net.ClientID;
import com.tc.net.groups.L1RemovedGroupMessage;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.MessageChannel;

public class CloseChannelHandler extends AbstractEventHandler<L1RemovedGroupMessage> {
  private static final Logger logger = LoggerFactory.getLogger(CloseChannelHandler.class);
  private final ChannelManager  channelManager;

  public CloseChannelHandler(ChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public void handleEvent(L1RemovedGroupMessage msg) {
    final ClientID cid = msg.getClientID();
    MessageChannel channel = channelManager.getChannel(cid.getChannelID());
    if (channel != null) {
      logger.warn("Close " + cid + " due to disconnect from stripe");
      channel.close();
    }
  }

}
