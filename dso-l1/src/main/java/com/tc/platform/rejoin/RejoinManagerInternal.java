/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ClientMessageChannel;

public interface RejoinManagerInternal extends RejoinManager {

  void start();

  void requestRejoin(ClientMessageChannel channel);

  boolean thisNodeJoined(ClientID newNodeId);

  int getRejoinCount();

  boolean isRejoinInProgress();
}
