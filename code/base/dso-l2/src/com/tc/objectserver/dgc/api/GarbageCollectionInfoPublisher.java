/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.util.ObjectIDSet;

public interface GarbageCollectionInfoPublisher {

  public void removeListener(GarbageCollectorEventListener listener);

  public void addListener(GarbageCollectorEventListener listener);

  public void fireGCStartEvent(GarbageCollectionInfo info);

  public void fireGCMarkEvent(GarbageCollectionInfo info);

  public void fireGCMarkResultsEvent(GarbageCollectionInfo info);

  public void fireGCRescue1CompleteEvent(GarbageCollectionInfo info);

  public void fireGCPausingEvent(GarbageCollectionInfo info);

  public void fireGCPausedEvent(GarbageCollectionInfo info);

  public void fireGCRescue2StartEvent(GarbageCollectionInfo info);

  public void fireGCMarkCompleteEvent(GarbageCollectionInfo info);

  public void fireGCDeleteEvent(GarbageCollectionInfo info);

  public void fireGCCycleCompletedEvent(GarbageCollectionInfo info, ObjectIDSet toDelete);

  public void fireGCCompletedEvent(GarbageCollectionInfo info);

  public void fireGCCanceledEvent(GarbageCollectionInfo info);
}
