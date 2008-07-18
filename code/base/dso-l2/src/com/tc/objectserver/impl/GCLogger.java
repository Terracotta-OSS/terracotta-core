/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.core.api.GarbageCollectionInfo;

import java.util.List;
import java.util.Set;

public class GCLogger {
  private TCLogger         logger;
  private final boolean    verboseGC;

  public GCLogger(TCLogger logger, boolean verboseGC) {
    this.logger = logger;
    this.verboseGC = verboseGC;
  }

  public void log_GCStart(long iteration) {
    if (verboseGC()) logGC("GC: START " + iteration);
  }

  public void log_markStart(Set managedIdSet) {
    if (verboseGC()) logGC("GC: pre-GC managed id count: " + managedIdSet.size());
  }

  public void log_markResults(Set gcResults) {
    if (verboseGC()) logGC("GC: pre-rescue GC results: " + gcResults.size());
  }

  public void log_quiescing() {
    if (verboseGC()) logGC("GC: quiescing...");
  }

  public void log_paused() {
    if (verboseGC()) logGC("GC: paused.");
  }

  public void log_rescue(int pass, Set objectIDs) {
    if (verboseGC()) logGC("GC: rescue pass " + pass + " on " + objectIDs.size() + " objects...");
  }

  public void log_sweep(Set toDelete) {
    if (verboseGC()) logGC("GC: deleting garbage: " + toDelete.size() + " objects");
  }

  public void log_notifyGCComplete() {
    if (verboseGC()) logGC("GC: notifying gc complete...");
  }


  public void log_GCComplete(GarbageCollectionInfo gcInfo, List rescueTimes) {
    if (verboseGC()) {
      for (int i = 0; i < rescueTimes.size(); i++) {
        logGC("GC: rescue " + (i + 1) + " time   : " + rescueTimes.get(i) + " ms.");
      }
      logGC("GC: paused gc time  : " + gcInfo.getPausedStageTime() + " ms.");
      logGC("GC: delete in-memory garbage time  : " + gcInfo.getDeleteStageTime() + " ms.");
      logGC("GC: total gc time   : " + gcInfo.getElapsedTime() + " ms.");
      logGC("GC: STOP " + gcInfo.getIteration());
    } else {
      logGC("GC: Complete : " + gcInfo);
    }
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  private void logGC(Object o) {
    logger.info(o);
  }
}
