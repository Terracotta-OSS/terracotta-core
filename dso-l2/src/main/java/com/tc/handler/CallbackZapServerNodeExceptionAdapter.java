/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.handler;

import org.slf4j.Logger;

import com.tc.logging.CallbackOnExitState;
import com.tc.objectserver.persistence.ClusterStatePersistor;

public class CallbackZapServerNodeExceptionAdapter extends CallbackDirtyDatabaseCleanUpAdapter {

  private final Logger consoleLogger;
  private String         consoleMessage = "This Terracotta server instance restarted because of a "
                                          + "conflict or communication failure with another Terracotta "
                                          + "server instance.";

  public CallbackZapServerNodeExceptionAdapter(Logger logger, Logger consoleLogger,
                                               ClusterStatePersistor clusterStateStore) {
    super(logger, clusterStateStore);
    this.consoleLogger = consoleLogger;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    super.callbackOnExit(state);
    consoleLogger.error(consoleMessage + "\n");
  }
}
