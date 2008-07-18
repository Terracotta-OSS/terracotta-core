/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;


public interface GarbageCollectionInfoPublisher {
  
  public void fireGCStartEvent(GarbageCollectionInfo info);
  
  public void fireGCMarkEvent(GarbageCollectionInfo info);
  
  public void fireGCMarkResultsEvent(GarbageCollectionInfo info);
  
  public void fireGCRescue1Event(GarbageCollectionInfo info);

  public void fireGCRescue1ResultsEvent(GarbageCollectionInfo info);
  
  public void fireGCPausingEvent(GarbageCollectionInfo info);
  
  public void fireGCPausedEvent(GarbageCollectionInfo info);
  
  public void fireGCRescue2Event(GarbageCollectionInfo info);
  
  public void fireGCRescue2ResultsEvent(GarbageCollectionInfo info);
  
  public void fireGCMarkCompleteEvent(GarbageCollectionInfo info);
  
  public void fireGCDeleteEvent(GarbageCollectionInfo info);
  
  public void fireGCCycleCompletedEvent(GarbageCollectionInfo info);
  
  public void fireGCCompletedEvent(GarbageCollectionInfo info);

}
