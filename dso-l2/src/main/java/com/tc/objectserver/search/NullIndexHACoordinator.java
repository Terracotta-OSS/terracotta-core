/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
