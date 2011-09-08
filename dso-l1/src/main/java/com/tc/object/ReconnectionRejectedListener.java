/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.protocol.tcm.ChannelEventListener;

/**
 * For Express Rejoin clients, upon reconnection rejected events, listeners at the ehcache layer will shutdown the
 * current L1 and spawn a new L1.
 */
public interface ReconnectionRejectedListener extends ChannelEventListener {

  void shutDown();

}
