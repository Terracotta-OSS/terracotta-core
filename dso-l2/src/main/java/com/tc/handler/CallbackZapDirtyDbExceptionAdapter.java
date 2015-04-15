/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.handler;

import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class CallbackZapDirtyDbExceptionAdapter extends CallbackDirtyDatabaseCleanUpAdapter {

  private static final TCProperties l2Props = TCPropertiesImpl.getProperties();
  private final TCLogger            consoleLogger;

  public CallbackZapDirtyDbExceptionAdapter(TCLogger logger, TCLogger consoleLogger,
                                            ClusterStatePersistor clusterStateStore) {
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
