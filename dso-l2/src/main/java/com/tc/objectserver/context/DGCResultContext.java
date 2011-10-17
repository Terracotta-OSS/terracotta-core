/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

import java.util.SortedSet;

public class DGCResultContext implements EventContext {
  private final SortedSet<ObjectID>   garbage;
  private final GarbageCollectionInfo info;

  public DGCResultContext(final SortedSet<ObjectID> garbage, final GarbageCollectionInfo info) {
    this.garbage = garbage;
    this.info = info;
  }

  public SortedSet<ObjectID> getGarbageIDs() {
    return garbage;
  }

  public int getGCIterationCount() {
    return this.info.getIteration();
  }

  public GarbageCollectionInfo getGCInfo() {
    return this.info;
  }

  @Override
  public String toString() {
    return "DGCResultContext [ " + this.info.getIteration() + " , " + getGarbageIDs().size() + " ]";
  }
}
