/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.MessageChannel;

public interface DSOChannelManagerEventListener {

  void channelCreated(MessageChannel channel);

  void channelRemoved(MessageChannel channel);

}
