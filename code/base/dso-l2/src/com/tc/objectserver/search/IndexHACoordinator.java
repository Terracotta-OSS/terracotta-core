/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;

import java.io.IOException;

public interface IndexHACoordinator extends IndexManager, StateChangeListener {

  public boolean syncIndex();

  public void setStateManager(StateManager stateManager);

  public void applyTempJournalsAndSwitch() throws IOException;

  public void applyIndexSync(String indexName, String fileName, byte[] fileData);

}
