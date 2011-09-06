/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;

public class CallbackZapServerNodeExceptionAdapter extends CallbackDirtyDatabaseCleanUpAdapter {

  private final TCLogger consoleLogger;
  private String         consoleMessage = "This Terracotta server instance shut down because of a "
                                          + "conflict or communication failure with another Terracotta "
                                          + "server instance. The database must be manually wiped before "
                                          + "it can be started and allowed to rejoin the cluster.";

  public CallbackZapServerNodeExceptionAdapter(TCLogger logger, TCLogger consoleLogger,
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
