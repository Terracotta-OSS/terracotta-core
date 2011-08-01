/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

import java.util.SortedSet;

public class PeriodicDGCResultContext extends DGCResultContext {

  private final GarbageCollectionInfo gcInfo;

  public PeriodicDGCResultContext(SortedSet<ObjectID> garbage, GarbageCollectionInfo gcInfo) {
    super(garbage);
    this.gcInfo = gcInfo;
  }

  public int getGCIterationCount() {
    return this.gcInfo.getIteration();
  }

  public GarbageCollectionInfo getGCInfo() {
    return this.gcInfo;
  }

  @Override
  public String toString() {
    return "PeriodicDGCResultContext [ " + this.gcInfo.getIteration() + " , " + getGarbageIDs().size() + " ]";
  }
}
