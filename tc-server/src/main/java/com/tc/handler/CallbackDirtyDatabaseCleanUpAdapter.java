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

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.objectserver.persistence.ClusterStatePersistor;

public class CallbackDirtyDatabaseCleanUpAdapter implements CallbackOnExitHandler {

  private final Logger logger;
  private final ClusterStatePersistor clusterStateStore;

  public CallbackDirtyDatabaseCleanUpAdapter(Logger logger, ClusterStatePersistor clusterStateStore) {
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
