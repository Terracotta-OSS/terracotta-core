/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.StageMonitor;
import com.tc.text.StringFormatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class StageMonitorImpl implements StageMonitor {

  private final String          name;
  private final StringFormatter formatter;

  private final List            snapshots = new ArrayList();
  private long                  begin     = System.currentTimeMillis();

  StageMonitorImpl(String name, StringFormatter formatter) {
    this.name = formatter.rightPad(30, name);
    this.formatter = formatter;
  }

  public synchronized void eventBegin(int queueDepth) {
    snapshots.add(new Snapshot(queueDepth));
  }

  public synchronized String dumpAndFlush() {
    long elapsed = System.currentTimeMillis() - begin;
    StringBuffer rv = new StringBuffer();
    dump(elapsed, rv);
    flush();
    return rv.toString();
  }

  private StringBuffer dump(long elapsed, StringBuffer buf) {
    Analysis an = analyze();
    buf.append(name).append("| period: ").append(formatter.leftPad(10, an.getElapsedTime())).append("ms.| events: ")
        .append(formatter.leftPad(10, an.getEventCount()));

    buf.append("| events/sec: ").append(formatter.leftPad(10, an.getEventsPerSecond()));

    buf.append("| Q depth, min: ").append(formatter.leftPad(10, an.getMinQueueDepth()));
    buf.append(", max: ").append(formatter.leftPad(10, an.getMaxQueueDepth()));
    buf.append(", avg: ").append(formatter.leftPad(10, an.getAvgQueueDepth()));

    return buf;
  }

  // XXX: Yeah, I know this is dumb.
  private Double safeDiv(long numerator, long denominator) {
    if (denominator > 0) {
      return new Double(((double) numerator) / ((double) denominator));
    } else {
      return new Double(-1);
    }
  }

  public synchronized Analysis analyze() {
    long elapsed = System.currentTimeMillis() - begin;
    int min = -1, max = 0;
    long sum = 0;
    for (Iterator i = snapshots.iterator(); i.hasNext();) {
      int qd = ((Snapshot) i.next()).getQueueDepth();
      if (qd < min || min < 0) min = qd;
      if (qd > max) max = qd;
      sum += qd;
    }

    return new AnalysisImpl(Long.valueOf(elapsed), Integer.valueOf(snapshots.size()), safeDiv(snapshots.size() * 1000,
                                                                                              elapsed),
                            Integer.valueOf(min), Integer.valueOf(max), safeDiv(sum, snapshots.size()));
  }

  public synchronized void flush() {
    snapshots.clear();
    begin = System.currentTimeMillis();
  }

  public static class AnalysisImpl implements Analysis {
    private final Number eventCount;
    private final Number eventsPerSecond;
    private final Number minQueueDepth;
    private final Number maxQueueDepth;
    private final Number avgQueueDepth;
    private final Number elapsedTime;

    private AnalysisImpl(Number elapsedTime, Number eventCount, Number eventsPerSecond, Number minQueueDepth,
                         Number maxQueueDepth, Number avgQueueDepth) {
      this.elapsedTime = elapsedTime;
      this.eventCount = eventCount;
      this.eventsPerSecond = eventsPerSecond;
      this.minQueueDepth = minQueueDepth;
      this.maxQueueDepth = maxQueueDepth;
      this.avgQueueDepth = avgQueueDepth;
    }

    public Number getElapsedTime() {
      return elapsedTime;
    }

    public Number getAvgQueueDepth() {
      return avgQueueDepth;
    }

    public Number getMaxQueueDepth() {
      return maxQueueDepth;
    }

    public Number getMinQueueDepth() {
      return minQueueDepth;
    }

    public Number getEventsPerSecond() {
      return eventsPerSecond;
    }

    public Number getEventCount() {
      return eventCount;
    }

  }

  private static class Snapshot {
    private final int queueDepth;

    private Snapshot(int queueDepth) {
      this.queueDepth = queueDepth;
    }

    public int getQueueDepth() {
      return queueDepth;
    }
  }
}
