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
package com.tc.objectserver.persistence;

import com.tc.l2.state.ServerMode;
import com.tc.l2.state.StateManager;

/**
 *
 */
public class ClusterPersistentState implements ServerPersistentState {
  
  private final ClusterStatePersistor persistor;

  public ClusterPersistentState(ClusterStatePersistor persistor) {
    this.persistor = persistor;
  }

  @Override
  public boolean isDBClean() {
    return persistor.isDBClean();
  }

  @Override
  public void setDBClean(boolean clean) {
    persistor.setDBClean(clean);
  }

  @Override
  public ServerMode getInitialMode() {
    return StateManager.convert(persistor.getInitialState());
  }
  
}
