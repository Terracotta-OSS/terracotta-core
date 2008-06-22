/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.object.persistence.api.SleepycatClusterStateMapStore;

public class CallbackDirtyDatabaseCleanUpAdapter implements CallbackOnExitHandler {

  private final TCLogger           logger;
  private final PersistentMapStore clusterStateStore;

  public CallbackDirtyDatabaseCleanUpAdapter(TCLogger logger, PersistentMapStore clusterStateStore) {
    this.logger = logger;
    this.clusterStateStore = clusterStateStore;
  }

  public void callbackOnExit(CallbackOnExitState state) {
    String errorMessage = "\n\nTerracotta Persistent-data startup exception:\n\n";
    System.err.print(errorMessage);
    state.getThrowable().printStackTrace(System.err);
    System.err.flush();

    logger.error("Marking the object db as dirty ...");
    // there is no harm in marking this even for non-persistent dbs, except it has no effect :)
    clusterStateStore.put(SleepycatClusterStateMapStore.DBKEY_STATE, SleepycatClusterStateMapStore.DB_STATE_DIRTY);
    state.setRestartNeeded();
  }
}
