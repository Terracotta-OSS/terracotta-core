/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateManager;

import java.io.IOException;

public class NullIndexHACoordinator extends NullIndexManager implements IndexHACoordinator {

  public boolean syncIndex() {
    return false;
  }

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

  public void applyIndexSync(String cacheName, String fileName, byte[] data) {
    //  
  }

}
