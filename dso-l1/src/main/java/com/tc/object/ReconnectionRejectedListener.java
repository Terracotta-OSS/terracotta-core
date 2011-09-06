/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.protocol.tcm.ChannelEventListener;

public interface ReconnectionRejectedListener extends ChannelEventListener {
  void shutDown();
}
