/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.net;

import com.tc.net.protocol.tcm.MessageChannel;

public interface DSOChannelManagerEventListener {

  void channelCreated(MessageChannel channel);

  void channelRemoved(MessageChannel channel);

}
