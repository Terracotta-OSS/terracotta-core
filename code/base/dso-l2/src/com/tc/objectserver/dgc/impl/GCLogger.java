/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.Assert;

import java.util.List;
import java.util.Set;

public class GCLogger {
  private final TCLogger logger;
  private final boolean  verboseGC;

  public GCLogger(TCLogger logger, boolean verboseGC) {
    Assert.assertNotNull(logger);
    this.logger = logger;
    this.verboseGC = verboseGC;
  }

  public void log_GCStart(long iteration, boolean fullGC) {
    if (verboseGC()) logGC("DGC: " + (fullGC ? "Full GC" : "YoungGen GC") + " START " + iteration);
  }

  public void log_markStart(long size) {
    if (verboseGC()) logGC("DGC: pre-GC managed id count: " + size);
  }

  public void log_markResults(long size) {
    if (verboseGC()) logGC("DGC: pre-rescue GC results: " + size);
  }

  public void log_quiescing() {
    if (verboseGC()) logGC("DGC: quiescing...");
  }

  public void log_paused() {
    if (verboseGC()) logGC("DGC: paused.");
  }

  public void log_rescue_complete(int pass, long count) {
    if (verboseGC()) logGC("DGC: rescue pass " + pass + " completed. gc candidates = " + count + " objects...");
  }

  public void log_rescue_start(int pass, long count) {
    if (verboseGC()) logGC("DGC: rescue pass " + pass + " on " + count + " objects...");
  }

  public void log_sweep(Set toDelete) {
    if (verboseGC()) logGC("DGC: deleting garbage: " + toDelete.size() + " objects");
  }

  public void log_notifyGCComplete() {
    if (verboseGC()) logGC("DGC: notifying gc complete...");
  }

  public void log_GCComplete(GarbageCollectionInfo gcInfo, List rescueTimes) {
    if (verboseGC()) {
      for (int i = 0; i < rescueTimes.size(); i++) {
        logGC("DGC: rescue " + (i + 1) + " time   : " + rescueTimes.get(i) + " ms.");
      }
      logGC("DGC: paused gc time  : " + gcInfo.getPausedStageTime() + " ms.");
      logGC("DGC: delete in-memory garbage time  : " + gcInfo.getDeleteStageTime() + " ms.");
      logGC("DGC: total mark cycle time   : " + gcInfo.getTotalMarkCycleTime() + " ms.");
      logGC("DGC: " + (gcInfo.isFullGC() ? "Full GC" : "YoungGen GC") + " STOP " + gcInfo.getIteration());
    } else {
      logGC("DGC: Complete : " + gcInfo);
    }
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  private void logGC(Object o) {
    logger.info(o);
  }
}
