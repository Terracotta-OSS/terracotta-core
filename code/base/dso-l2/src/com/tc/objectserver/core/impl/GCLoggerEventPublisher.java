/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.impl.GCLogger;

public class GCLoggerEventPublisher extends GarbageCollectorEventListenerAdapter {
  
  private final GCLogger gcLogger;

  public GCLoggerEventPublisher( TCLogger logger, boolean verboseGC ) {
    gcLogger = new GCLogger(logger, verboseGC );
  }
 

  public void garbageCollectorMark(GarbageCollectionInfo info) {
    gcLogger.log_markStart(info.getManagedIDs());
  }

  public void garbageCollectorMarkResults(GarbageCollectionInfo info) {
    gcLogger.log_markResults(info.getGcResults());
  }

  public void garbageCollectorRescue1(GarbageCollectionInfo info) {
    gcLogger.log_rescue(1, info.getGcResults());  
  }

  public void garbageCollectorPausing(GarbageCollectionInfo info) {
    gcLogger.log_quiescing();
  }

  public void garbageCollectorPaused(GarbageCollectionInfo info) {
    gcLogger.log_paused();
  }

  public void garbageCollectorRescue2(GarbageCollectionInfo info) {
    gcLogger.log_rescue(2, info.getGcResults());  
  }

  public void garbageCollectorMarkComplete(GarbageCollectionInfo info) {
    gcLogger.log_sweep(info.getDeleted());
    gcLogger.log_notifyGCComplete();
  }

  public void garbageCollectorCycleCompleted(GarbageCollectionInfo info) {
    gcLogger.log_GCComplete(info, info.getRescueTimes());
  }

}
