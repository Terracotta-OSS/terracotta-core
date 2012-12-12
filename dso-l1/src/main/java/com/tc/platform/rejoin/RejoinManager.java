/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;


public interface RejoinManager {

  boolean isRejoinEnabled();

  void addListener(RejoinLifecycleListener listener);

  void removeListener(RejoinLifecycleListener listener);

  void shutdown();
}
