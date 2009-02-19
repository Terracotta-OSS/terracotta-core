/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.impl.GarbageCollectionID;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.dgc.impl.GarbageCollectionInfoPublisherImpl;
import com.tc.util.UUID;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

  private static final GarbageCollectionInfo   NULL_GARBAGE_COLLECTION_INFO = new GarbageCollectionInfo(new GarbageCollectionID(-1,UUID.getUUID()
                                                                                                                                    .toString()),true);

  private final int                            gcIteration;
  private final SortedSet<ObjectID>            gcedOids;
  private final GarbageCollectionInfo          gcInfo;
  private final GarbageCollectionInfoPublisher gcPublisher;

  public GCResultContext(int gcIteration, SortedSet gcedOids) {
    this(gcIteration, gcedOids, NULL_GARBAGE_COLLECTION_INFO,
         GarbageCollectionInfoPublisherImpl.NULL_GARBAGE_COLLECCTION_INFO_PUBLISHER);
  }

  public GCResultContext(int gcIteration, SortedSet gcedOids, GarbageCollectionInfo gcInfo,
                         GarbageCollectionInfoPublisher gcPublisher) {
    this.gcIteration = gcIteration;
    this.gcedOids = gcedOids;
    this.gcInfo = gcInfo;
    this.gcPublisher = gcPublisher;
  }

  public int getGCIterationCount() {
    return this.gcIteration;
  }

  public SortedSet<ObjectID> getGCedObjectIDs() {
    return this.gcedOids;
  }

  public GarbageCollectionInfo getGCInfo() {
    return this.gcInfo;
  }

  public GarbageCollectionInfoPublisher getGCPublisher() {
    return this.gcPublisher;
  }

  @Override
  public String toString() {
    return "GCResultContext [ " + this.gcIteration + " , " + this.gcedOids.size() + " ]";
  }
}
