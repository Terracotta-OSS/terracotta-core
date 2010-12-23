/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.OrderedEventContext;
import com.tc.net.NodeID;

import java.io.File;

public interface IndexSyncContext extends OrderedEventContext {
  //

  public NodeID getNodeID();

  public void setSequenceID(long sequenceID);

  public File syncFile();

  public String getCacheName();

  public boolean hasMore();

}
