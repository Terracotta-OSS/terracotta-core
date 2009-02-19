/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.logging.TCLogger;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.util.Assert;

import java.util.List;

public class GCLogger {
  private final TCLogger logger;
  private final boolean  verboseGC;
  private final String   prefix;

  public GCLogger(TCLogger logger, boolean verboseGC) {
    this("DGC", logger, verboseGC);
  }

  public GCLogger(String prefix, TCLogger logger, boolean verboseGC) {
    Assert.assertNotNull(logger);
    this.logger = logger;
    this.verboseGC = verboseGC;
    this.prefix = prefix;
  }

  public void log_start(long iteration, boolean fullGC) {
    if (verboseGC()) logGC(iteration, (fullGC ? "Full GC" : "YoungGen GC") + " start ");
  }

  public void log_markStart(long iteration, long size) {
    if (verboseGC()) logGC(iteration, "pre-GC managed id count: " + size);
  }

  public void log_markResults(long iteration, long size) {
    if (verboseGC()) logGC(iteration, "pre-rescue GC results: " + size);
  }

  public void log_quiescing(long iteration) {
    if (verboseGC()) logGC(iteration, "quiescing...");
  }

  public void log_paused(long iteration) {
    if (verboseGC()) logGC(iteration, "paused.");
  }

  public void log_rescue_complete(long iteration, int pass, long count) {
    if (verboseGC()) logGC(iteration, "rescue pass " + pass + " completed. gc candidates = " + count + " objects...");
  }

  public void log_rescue_start(long iteration, int pass, long count) {
    if (verboseGC()) logGC(iteration, "rescue pass " + pass + " on " + count + " objects...");
  }

  public void log_markComplete(long iteration, long count) {
    if (verboseGC()) logGC(iteration, "deleting garbage: " + count + " objects");
  }

  public void log_deleteStart(long iteration, int toDeleteSize) {
    if (verboseGC()) logGC(iteration, "delete start : " + toDeleteSize + " objects");
  }

  public void log_cycleComplete(long iteration, GarbageCollectionInfo gcInfo, List rescueTimes) {
    if (verboseGC()) {
      logGC(iteration, "notifying gc complete...");
      for (int i = 0; i < rescueTimes.size(); i++) {
        logGC(iteration, "rescue " + (i + 1) + " time   : " + rescueTimes.get(i) + " ms.");
      }
      logGC(iteration, "paused gc time  : " + gcInfo.getPausedStageTime() + " ms.");
      logGC(iteration, "delete in-memory garbage time  : " + gcInfo.getDeleteStageTime() + " ms.");
      logGC(iteration, "total mark cycle time   : " + gcInfo.getTotalMarkCycleTime() + " ms.");
      logGC(iteration, "" + (gcInfo.isFullGC() ? "Full GC" : "YoungGen GC") + " STOP " + gcInfo.getIteration());
    } else {
      logGC(iteration, "complete : " + gcInfo);
    }
  }

  public void log_complete(long iteration, int deleteGarbageSize, long elapsed) {
    if (verboseGC()) {
      logGC(iteration, "delete complete : removed " + deleteGarbageSize + " objects in " + elapsed + " ms.");
    }
  }

  public void log_canceled(long iteration) {
    if (verboseGC()) {
      logGC(iteration, "canceled");
    }
  }

  public boolean verboseGC() {
    return verboseGC;
  }

  private void logGC(long iteration, String msg) {
    logger.info(prefix + "[ " + iteration + " ] " + msg);
  }
}
