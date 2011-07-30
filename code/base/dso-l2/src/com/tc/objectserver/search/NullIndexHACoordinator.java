/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;
import com.tc.net.NodeID;

import java.io.IOException;
import java.io.InputStream;

public class NullIndexHACoordinator extends NullIndexManager implements IndexHACoordinator {

  public void setStateManager(StateManager stateManager) {
    //
  }

  @SuppressWarnings("unused")
  public void applyTempJournalsAndSwitch() throws IOException {
    //
  }

  public void l2StateChanged(StateChangedEvent sce) {
    //
  }

  public void applyIndexSync(String cacheName, String fileName, byte[] data, boolean isTCFile, boolean isLast) {
    //
  }

  public void nodeJoined(NodeID nodeID) {
    //
  }

  public void nodeLeft(NodeID nodeID) {
    //
  }

  public void doSyncPrepare() {
    //
  }

  public InputStream getIndexFile(String name) {
    throw new AssertionError();
  }

}
