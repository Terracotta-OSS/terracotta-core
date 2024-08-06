/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
