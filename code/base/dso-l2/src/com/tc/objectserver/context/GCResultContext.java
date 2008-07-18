/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoPublisher;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

  private final int                 gcIteration;
  private final SortedSet<ObjectID> gcedOids;
  private final GarbageCollectionInfo gcInfo;
  private final GarbageCollectionInfoPublisher gcPublisher;

  public GCResultContext(int gcIteration, GarbageCollectionInfo gcInfo, GarbageCollectionInfoPublisher gcPublisher, SortedSet gcedOids) {
    this.gcIteration = gcIteration;
    this.gcedOids = gcedOids;
    this.gcInfo = gcInfo;
    this.gcPublisher = gcPublisher;
  }

  public int getGCIterationCount() {
    return gcIteration;
  }

  public SortedSet<ObjectID> getGCedObjectIDs() {
    return gcedOids;
  }
  
  public GarbageCollectionInfo getGCInfo() {
    return gcInfo;
  }

  public GarbageCollectionInfoPublisher getGCPublisher() {
    return gcPublisher;
  }

  public String toString() {
    return "GCResultContext [ " + gcIteration + " , " + gcedOids.size() + " ]";
  }
}
