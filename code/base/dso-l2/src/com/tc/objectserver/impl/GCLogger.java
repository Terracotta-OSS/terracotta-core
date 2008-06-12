/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.api.GCStats;
import com.tc.stats.LossyStack;

import java.util.List;
import java.util.Set;

public class GCLogger {
  private TCLogger         logger;
  private final LossyStack gcHistory = new LossyStack(1500);
  private final boolean    verboseGC;

  public GCLogger(TCLogger logger, boolean verboseGC) {
    this.logger = logger;
    this.verboseGC = verboseGC;
  }

  public void push(Object obj) {
    gcHistory.push(obj);
  }

  public GCStats[] getGarbageCollectorStats() {
    return (GCStats[]) gcHistory.toArray(new GCStats[gcHistory.depth()]);
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

  public void log_GCDisabled() {
    if (verboseGC()) logGC("GC: Not running gc since its disabled...");
  }

  public void log_GCComplete(GCStats gcStats, List rescueTimes) {
    if (verboseGC()) {
      for (int i = 0; i < rescueTimes.size(); i++) {
        logGC("GC: rescue " + (i + 1) + " time   : " + rescueTimes.get(i) + " ms.");
      }
      logGC("GC: paused gc time  : " + gcStats.getPausedTime() + " ms.");
      logGC("GC: delete in-memory garbage time  : " + gcStats.getDeleteTime() + " ms.");
      logGC("GC: total gc time   : " + gcStats.getElapsedTime() + " ms.");
      logGC("GC: STOP " + gcStats.getIteration());
    } else {
      logGC("GC: Complete : " + gcStats);
    }
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  private void logGC(Object o) {
    logger.info(o);
  }
}
