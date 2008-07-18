/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoPublisher;
import com.tc.objectserver.core.impl.GarbageCollectionInfoImpl;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

  private static final GarbageCollectionInfo          NULL_GARBAGE_COLLECTION_INFO           = new GarbageCollectionInfoImpl(
                                                                                                                             -1);

  private static final GarbageCollectionInfoPublisher NULL_GARBAGE_COLLECTION_INFO_PUBLISHER = new GarbageCollectionInfoPublisher() {
                                                                                               public void fireGCStartEvent(
                                                                                                                            GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCMarkEvent(
                                                                                                                           GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCMarkResultsEvent(
                                                                                                                                  GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCRescue1CompleteEvent(
                                                                                                                                      GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCPausingEvent(
                                                                                                                              GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCPausedEvent(
                                                                                                                             GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCRescue2StartEvent(
                                                                                                                                   GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCMarkCompleteEvent(
                                                                                                                                   GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCDeleteEvent(
                                                                                                                             GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCCycleCompletedEvent(
                                                                                                                                     GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }

                                                                                               public void fireGCCompletedEvent(
                                                                                                                                GarbageCollectionInfo info) {
                                                                                                 // do nothing
                                                                                               }
                                                                                             };

  private final int                                   gcIteration;
  private final SortedSet<ObjectID>                   gcedOids;
  private final GarbageCollectionInfo                 gcInfo;
  private final GarbageCollectionInfoPublisher        gcPublisher;

  public GCResultContext(int gcIteration, SortedSet gcedOids) {
    this(gcIteration, gcedOids, NULL_GARBAGE_COLLECTION_INFO, NULL_GARBAGE_COLLECTION_INFO_PUBLISHER);
  }

  public GCResultContext(int gcIteration, SortedSet gcedOids, GarbageCollectionInfo gcInfo,
                         GarbageCollectionInfoPublisher gcPublisher) {
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
