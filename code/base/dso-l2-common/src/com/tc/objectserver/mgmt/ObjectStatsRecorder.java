/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.mgmt;

import com.tc.statistics.util.NullStatsRecorder;
import com.tc.statistics.util.StatsPrinter;
import com.tc.statistics.util.StatsRecorder;

import java.text.MessageFormat;

public class ObjectStatsRecorder {
  private boolean                    faultDebug;
  private StatsRecorder              faultStatsRecorder;

  private boolean                    requestDebug;
  private StatsRecorder              requestStatsRecorder;

  private boolean                    flushDebug;
  private StatsRecorder              flushStatsRecorder;

  private boolean                    broadcastDebug;
  private StatsRecorder              broadcastStatsRecorder;

  private boolean                    commitDebug;
  private StatsRecorder              commmitStatsRecorder;

  private static final StatsRecorder NULL_RECORDER = new NullStatsRecorder();

  public ObjectStatsRecorder() {
    this(false, false, false, false, false);
  }

  public ObjectStatsRecorder(boolean faultDebug, boolean requestDebug, boolean flushDebug, boolean broadcastDebug,
                             boolean commitDebug) {
    setFaultDebug(faultDebug);
    setRequestDebug(requestDebug);
    setFlushDebug(flushDebug);
    setBroadcastDebug(broadcastDebug);
    setCommitDebug(commitDebug);
  }

  public synchronized boolean getFaultDebug() {
    return faultDebug;
  }

  public synchronized void setFaultDebug(boolean faultDebug) {
    this.faultDebug = faultDebug;
    if (faultStatsRecorder != null) {
      faultStatsRecorder.finish();
    }
    if (faultDebug) {
      faultStatsRecorder = new StatsPrinter(new MessageFormat("Faulted from disk in the Last {0} ms"),
                                            new MessageFormat(" {0} instances"), true);
    } else {
      faultStatsRecorder = NULL_RECORDER;
    }
  }

  public void updateFaultStats(String type) {
    faultStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized void setRequestDebug(boolean requestDebug) {
    this.requestDebug = requestDebug;
    if (requestStatsRecorder != null) {
      requestStatsRecorder.finish();
    }
    if (requestDebug) {
      requestStatsRecorder = new StatsPrinter(new MessageFormat("Object-requests in the Last {0} ms"),
                                              new MessageFormat(" {0} instances"), true);
    } else {
      requestStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getRequestDebug() {
    return requestDebug;
  }

  public void updateRequestStats(String type) {
    requestStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized boolean getFlushDebug() {
    return flushDebug;
  }

  public synchronized void setFlushDebug(boolean flushDebug) {
    this.flushDebug = flushDebug;
    if (flushStatsRecorder != null) {
      flushStatsRecorder.finish();
    }
    if (flushDebug) {
      flushStatsRecorder = new StatsPrinter(new MessageFormat("Flushed to disk in the Last {0} ms"),
                                            new MessageFormat(" {0} instances"), true);
    } else {
      flushStatsRecorder = NULL_RECORDER;
    }
  }

  public void updateFlushStats(String type) {
    flushStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized void setBroadcastDebug(boolean broadcastDebug) {
    this.broadcastDebug = broadcastDebug;
    if (broadcastStatsRecorder != null) {
      broadcastStatsRecorder.finish();
    }
    if (broadcastDebug) {
      broadcastStatsRecorder = new StatsPrinter(new MessageFormat("Broadcasted in the Last {0} ms"),
                                                new MessageFormat(" {0} instances"), true);
    } else {
      broadcastStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getBroadcastDebug() {
    return broadcastDebug;
  }

  public void updateBroadcastStats(String type) {
    broadcastStatsRecorder.updateStats(type, StatsRecorder.SINGLE_INCR);
  }

  public synchronized void setCommitDebug(boolean commitDebug) {
    this.commitDebug = commitDebug;
    if (commmitStatsRecorder != null) {
      commmitStatsRecorder.finish();
    }
    if (commitDebug) {
      commmitStatsRecorder = new StatsPrinter(new MessageFormat("Commits in the Last {0} ms"),
                                              new MessageFormat(
                                              // hate this stupid formatter, can't figure how to prefix with space
                                                                // " count = {0,number,000000}   bytes = {1,number,0000000}   new = {2,number, 0000}"
                                                                " count = {0}   bytes = {1}   new = {2}"), true);
    } else {
      commmitStatsRecorder = NULL_RECORDER;
    }
  }

  public synchronized boolean getCommitDebug() {
    return commitDebug;
  }

  public void updateCommitStats(String type, int length, boolean isNew) {
    commmitStatsRecorder.updateStats(type, new long[] { 1, length, (isNew ? 1 : 0) });
  }

}
