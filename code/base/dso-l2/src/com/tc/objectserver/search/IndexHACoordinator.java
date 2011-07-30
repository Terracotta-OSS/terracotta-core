/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.l2.state.StateChangeListener;

import java.io.IOException;
import java.io.InputStream;

public interface IndexHACoordinator extends IndexManager, StateChangeListener {

  public InputStream getIndexFile(String indexName, String fileName) throws IOException;

  public void doSyncPrepare();

  public void applyIndexSync(String indexName, String fileName, byte[] fileData, boolean isTCFile, boolean isLast);

  public void applyTempJournalsAndSwitch() throws IOException;

}
