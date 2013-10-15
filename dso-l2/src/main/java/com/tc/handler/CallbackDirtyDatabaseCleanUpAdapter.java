/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.ClusterStatePersistor;

public class CallbackDirtyDatabaseCleanUpAdapter implements CallbackOnExitHandler {

  private final TCLogger              logger;
  private final ClusterStatePersistor clusterStateStore;

  public CallbackDirtyDatabaseCleanUpAdapter(TCLogger logger, ClusterStatePersistor clusterStateStore) {
    this.logger = logger;
    this.clusterStateStore = clusterStateStore;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    logger.error("Marking the object db as dirty ...");
    state.setRestartNeeded();
    clusterStateStore.setDBClean(false);
  }
}
