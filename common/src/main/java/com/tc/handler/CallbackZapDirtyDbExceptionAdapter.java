/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class CallbackZapDirtyDbExceptionAdapter extends CallbackDirtyDatabaseCleanUpAdapter {

  private static final TCProperties l2Props = TCPropertiesImpl.getProperties();
  private final TCLogger            consoleLogger;

  public CallbackZapDirtyDbExceptionAdapter(TCLogger logger, TCLogger consoleLogger,
                                            PersistentMapStore clusterStateStore) {
    super(logger, clusterStateStore);
    this.consoleLogger = consoleLogger;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    super.callbackOnExit(state);
    boolean autoDelete = l2Props.getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE, true);
    boolean autoRestart = l2Props.getBoolean(TCPropertiesConsts.L2_NHA_AUTORESTART, true);
    if (autoDelete && autoRestart) {
      consoleLogger.error(CallbackHandlerResources.getDirtyDBAutodeleteAutoRestartZapMessage() + "\n");
    } else if (autoDelete && !autoRestart) {
      consoleLogger.error(CallbackHandlerResources.getDirtyDBAutodeleteZapMessage() + "\n");
    } else if (!autoDelete && autoRestart) {
      consoleLogger.error(CallbackHandlerResources.getDirtyDBAutoRestartZapMessage() + "\n");
    } else {
      consoleLogger.error(CallbackHandlerResources.getDirtyDBZapMessage() + "\n");
    }

  }
}
