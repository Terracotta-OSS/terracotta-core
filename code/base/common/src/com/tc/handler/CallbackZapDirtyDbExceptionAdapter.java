/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;

public class CallbackZapDirtyDbExceptionAdapter extends CallbackDirtyDatabaseCleanUpAdapter {

  private final TCLogger consoleLogger;
  private String         consoleMessage = "This Terracotta server instance shut down because it went into "
                                          + "standby mode with an out-of-date database. The database must "
                                          + "be manually wiped before the Terracotta server instance can be "
                                          + "started and allowed to rejoin the cluster.";

  public CallbackZapDirtyDbExceptionAdapter(TCLogger logger, TCLogger consoleLogger,
                                            PersistentMapStore clusterStateStore) {
    super(logger, clusterStateStore);
    this.consoleLogger = consoleLogger;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    super.callbackOnExit(state);
    consoleLogger.error(consoleMessage + "\n");
  }

}
