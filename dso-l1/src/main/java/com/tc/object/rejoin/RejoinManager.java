/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.rejoin;

import com.tc.net.protocol.transport.ReconnectionRejectedHandler;

public interface RejoinManager {

  boolean isRejoinEnabled();

  void addListener(RejoinLifecycleListener listener);

  void removeListener(RejoinLifecycleListener listener);

  ReconnectionRejectedHandler getReconnectionRejectedHandler();

}
