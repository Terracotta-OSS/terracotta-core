/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;

import java.util.Collection;

/**
 * provides the sessionIDs
 * 
 * @author steve
 */
public interface ChannelManager {
  public MessageChannelInternal getChannel(ChannelID id);

  public void routeChannelStateChanges(Sink sink);

  public MessageChannelInternal[] getChannels();

  public boolean isValidID(ChannelID channelID);
  
  public void addEventListener(ChannelManagerEventListener listener);

  public Collection getAllChannelIDs();
}
