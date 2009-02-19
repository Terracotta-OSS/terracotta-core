/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.dgc.api.GarbageCollectionInfo;

public class GCLoggerEventPublisher extends GarbageCollectorEventListenerAdapter {

  private final GCLogger gcLogger;

  public GCLoggerEventPublisher(GCLogger logger) {
    gcLogger = logger;
  }
  
  @Override
  public void garbageCollectorStart(GarbageCollectionInfo info) {
    gcLogger.log_start(info.getIteration(), info.isFullGC());
  }

  @Override
  public void garbageCollectorMark(GarbageCollectionInfo info) {
    gcLogger.log_markStart(info.getIteration(), info.getBeginObjectCount());
  }

  @Override
  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
    gcLogger.log_markResults(info.getIteration(), info.getPreRescueCount());
  }

  @Override
  public void garbageCollectorRescue1Complete(GarbageCollectionInfo info) {
    gcLogger.log_rescue_complete(info.getIteration(), 1, info.getRescue1Count());
  }

  @Override
  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    gcLogger.log_quiescing(info.getIteration());
  }

  @Override
  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    gcLogger.log_paused(info.getIteration());
  }

  @Override
  public void garbageCollectorRescue2Start(GarbageCollectionInfo info) {
    gcLogger.log_rescue_start(info.getIteration(), 2, info.getCandidateGarbageCount());
  }

  @Override
  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    gcLogger.log_markComplete( info.getIteration(), info.getCandidateGarbageCount());
  }

  @Override
  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
    gcLogger.log_cycleComplete(info.getIteration(), info, info.getRescueTimes());
  }

  @Override
  public void garbageCollectorDelete(GarbageCollectionInfo info) {
    gcLogger.log_deleteStart(info.getIteration(), info.getDeleted().size());
  }
  
  @Override
  public void garbageCollectorCompleted(GarbageCollectionInfo info) {
    gcLogger.log_complete(info.getIteration(), info.getDeleted().size(), info.getElapsedTime());
  }

  @Override
  public void garbageCollectorCanceled(GarbageCollectionInfo info) {
    gcLogger.log_canceled(info.getIteration());
  }

  
  

}
