/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.state;

public interface StateSyncManager {

  void objectSyncComplete();

  void indexSyncComplete();

  void setStateManager(StateManager stateManager);

}
