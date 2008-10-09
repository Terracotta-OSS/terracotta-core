/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.util;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.ThreadUtil;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class StatsPrinter extends Thread implements StatsRecorder {
  private final static TCLogger                        statsLogger  = TCLogging.getLogger(StatsPrinter.class);
  private final long                                   timeInterval;
  private final ConcurrentHashMap<String, StatsRecord> statsRecords = new ConcurrentHashMap();
  private final MessageFormat                          formatLine;
  private final String                                 header;
  private volatile int                                 keyMaxSize   = 0;
  private final boolean                                printTotal;

  public StatsPrinter(String name, long timeInterval, MessageFormat header, MessageFormat formatLine, boolean printTotal) {
    super(name);
    this.printTotal = printTotal;
    this.setDaemon(true);
    this.timeInterval = timeInterval;
    this.header = header.format(new Long[] { timeInterval });
    this.formatLine = formatLine;
    this.start();
  }

  public void updateStats(String key, long... counters) {
    StatsRecord r = get(key);
    r.update(counters);
  }

  private StatsRecord get(String key) {
    StatsRecord r = statsRecords.get(key);
    if (r == null) {
      synchronized (this) {
        r = statsRecords.get(key);
        if (r == null) {
          statsRecords.put(key, (r = new StatsRecord(key)));
          updateKeyMaxSize(key.length());
        }
      }
    }
    return r;
  }

  private void updateKeyMaxSize(int length) {
    if (keyMaxSize < length) {
      keyMaxSize = length;
    }
  }

  public void run() {
    while (true) {
      ThreadUtil.reallySleep(timeInterval);
      statsLogger.info(header);
      statsLogger.info("==========================================================");
      StatsRecord total = new StatsRecord("TOTAL");
      for (Iterator i = statsRecords.entrySet().iterator(); i.hasNext();) {
        Map.Entry e = (Entry) i.next();
        StatsRecord r = (StatsRecord) e.getValue();
        printDetailsIfNecessary(r, total);
      }
      if (printTotal) printDetailsIfNecessary(total, null);
    }
  }

  public void printDetailsIfNecessary(StatsRecord toPrint, StatsRecord total) {
    long[] counters = toPrint.getAllAndReset();
    if (counters.length != 0 && counters[0] != 0) {
      statsLogger.info(createLogString(toPrint.getKey(), counters));
      if (total != null) total.update(counters);
    }
  }

  private String createLogString(String name, long[] counters) {
    StringBuffer sb = new StringBuffer();
    appendFixedSpaceString(sb, name, keyMaxSize + 1);
    sb.append(": ");
    return formatLine.format(toObjectArray(counters), sb, new FieldPosition(0)).toString();
  }

  // XXX::This is stupid, message formatter doesn't let me print fixed length number so converting to string
  // TODO:: If you find the right format string to print fixed length numbers, return Longs instead of Strings
  private Object[] toObjectArray(long[] counters) {
    Object ret[] = new Object[counters.length];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = appendFixedSpaceString(new StringBuffer(), String.valueOf(counters[i]), 8);
    }
    return ret;
  }

  private static StringBuffer appendFixedSpaceString(StringBuffer sb, String msg, int length) {
    int spaces = Math.max(length - msg.length(), 0);
    sb.append(msg);
    while (spaces-- > 0) {
      sb.append(' ');
    }
    return sb;
  }

  private static final class StatsRecord {
    private final String                           key;
    private final CopyOnWriteArrayList<AtomicLong> counterList = new CopyOnWriteArrayList<AtomicLong>();

    public StatsRecord(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public void update(long... counters) {
      if (counters.length > counterList.size()) {
        initCounters(counters.length);
      }
      for (int i = 0; i < counters.length; i++) {
        counterList.get(i).addAndGet(counters[i]);
      }
    }

    private void initCounters(int num) {
      while (counterList.size() < num) {
        counterList.add(new AtomicLong());
      }
    }

    public long[] getAllAndReset() {
      long ret[] = new long[counterList.size()];
      int idx = 0;
      for (AtomicLong counter : counterList) {
        ret[idx++] = counter.getAndSet(0);
      }
      return ret;
    }
  }
}
