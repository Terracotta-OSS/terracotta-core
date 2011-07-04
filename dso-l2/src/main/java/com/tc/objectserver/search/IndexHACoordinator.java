/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.state.StateChangeListener;

import java.io.IOException;

public interface IndexHACoordinator extends IndexManager, StateChangeListener {

  public void doSyncPrepare();

  public void applyIndexSync(String indexName, String fileName, byte[] fileData);

  public void applyTempJournalsAndSwitch() throws IOException;

}
