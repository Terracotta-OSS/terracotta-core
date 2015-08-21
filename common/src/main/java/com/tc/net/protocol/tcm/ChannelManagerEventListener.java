/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ChannelManagerEventListener {

  void channelCreated(MessageChannel channel);

  void channelRemoved(MessageChannel channel);

}
