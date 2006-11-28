/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ChannelManagerEventListener {

  void channelCreated(MessageChannel channel);

  void channelRemoved(MessageChannel channel);

}
