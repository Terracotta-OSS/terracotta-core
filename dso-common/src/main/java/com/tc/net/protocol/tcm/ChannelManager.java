/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
