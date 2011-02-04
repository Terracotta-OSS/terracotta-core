/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.net.NodeID;

import java.io.File;

public class NullIndexSyncContext implements IndexSyncContext {

  public String getCachename() {
    return "";
  }

  public String getFilename() {
    return "";
  }

  public NodeID getNodeID() {
    return null;
  }

  public boolean hasMore() {
    return false;
  }

  public void setSequenceID(long sequenceID) {
    //
  }

  public File syncFile() {
    return null;
  }

  public long getSequenceID() {
    return 0;
  }

  public int getTotalFilesSynced() {
    return 0;
  }

  public int getTotalFilesToSync() {
    return 0;
  }

}
