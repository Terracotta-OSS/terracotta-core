/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.net.protocol.tcm;

import java.util.Set;

/**
 * provides the sessionIDs
 *
 * @author steve
 */
public interface ChannelManager {
  public MessageChannelInternal getChannel(ChannelID id);

  public MessageChannelInternal[] getChannels();

  public boolean isValidID(ChannelID channelID);

  public void addEventListener(ChannelManagerEventListener listener);

  public Set getAllChannelIDs();

  public void closeAllChannels();
}
