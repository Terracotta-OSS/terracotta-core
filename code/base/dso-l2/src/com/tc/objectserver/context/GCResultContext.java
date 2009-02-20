/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

 
  private final SortedSet<ObjectID>            gcedOids;
  private final GarbageCollectionInfo          gcInfo;

  public GCResultContext(SortedSet gcedOids, GarbageCollectionInfo gcInfo) {
    this.gcedOids = gcedOids;
    this.gcInfo = gcInfo;
  }

  public int getGCIterationCount() {
    return this.gcInfo.getIteration();
  }

  public SortedSet<ObjectID> getGCedObjectIDs() {
    return this.gcedOids;
  }

  public GarbageCollectionInfo getGCInfo() {
    return this.gcInfo;
  }

  @Override
  public String toString() {
    return "GCResultContext [ " + this.gcInfo.getIteration() + " , " + this.gcedOids.size() + " ]";
  }
}
