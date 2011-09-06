/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;

public interface L2IndexStateListener {

  public void indexSyncStartFor(NodeID nodeID);

  public void indexFilesFor(NodeID nodeID, int indexFiles);

  public void indexSyncCompleteFor(NodeID nodeID);

}
