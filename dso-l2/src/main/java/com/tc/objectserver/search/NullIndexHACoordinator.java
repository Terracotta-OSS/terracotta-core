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
package com.tc.objectserver.search;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;

import java.io.IOException;

public class NullIndexHACoordinator extends NullIndexManager implements IndexHACoordinator {

  public void setStateManager(StateManager stateManager) {
    //
  }

  @Override
  public void applyTempJournalsAndSwitch() throws IOException {
    //
  }

  @Override
  public void l2StateChanged(StateChangedEvent sce) {
    //
  }

  @Override
  public void applyIndexSync(String cacheName, String indexId, String fileName, byte[] data, boolean isTCFile,
                             boolean isLast) {
    //
  }

  public void nodeJoined(NodeID nodeID) {
    //
  }

  public void nodeLeft(NodeID nodeID) {
    //
  }

  @Override
  public void doSyncPrepare() {
    //
  }

  @Override
  public int getNumberOfIndexesPerCache() {
    return 0;
  }

}
