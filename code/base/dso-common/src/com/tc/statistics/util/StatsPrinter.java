/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.util;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class StatsPrinter implements StatsRecorder, Runnable {
  private final static TCLogger   statsLogger  = TCLogging.getLogger(StatsPrinter.class);
  private final long              timeInterval;
  private final ConcurrentHashMap statsRecords = new ConcurrentHashMap();
  private final MessageFormat     formatLine;
  private final String            header;
  private volatile int            keyMaxSize   = 0;
  private boolean                 printHeader;
  private final boolean           printTotal;
  private boolean                 finished     = false;

  private static Aggregator       aggregator;

  public StatsPrinter(String name, long timeInterval, MessageFormat header, MessageFormat formatLine, boolean printTotal) {
    this.printTotal = printTotal;
    this.timeInterval = timeInterval;
    this.header = header.format(new Long[] { new Long(timeInterval) });
    this.formatLine = formatLine;

    Thread t = new Thread(this, name);
    t.setDaemon(true);
    t.start();
  }

  public StatsPrinter(MessageFormat header, MessageFormat formatLine, boolean printTotal) {
    this.printTotal = printTotal;
    this.timeInterval = -1;
    this.header = header.format(new Long[] { new Long(Aggregator.timeInterval) });
    this.formatLine = formatLine;

    getAggregator().addStatsPrinter(this);
  }

  private Aggregator getAggregator() {
    if (aggregator == null) {
      aggregator = new Aggregator();
    }
    return aggregator;
  }

  public synchronized void finish() {
    this.finished = true;
  }

  private synchronized boolean isFinished() {
    return finished;
  }

  public void updateStats(String key, long[] counters) {
    StatsRecord r = get(key);
    r.update(counters);
  }

  private StatsRecord get(String key) {
    StatsRecord r = (StatsRecord) statsRecords.get(key);
    if (r == null) {
      synchronized (this) {
        r = (StatsRecord) statsRecords.get(key);
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

  void print() {
    printHeader = true;
    StatsRecord total = new StatsRecord("TOTAL");
    synchronized (statsLogger) {
      for (Iterator i = statsRecords.entrySet().iterator(); i.hasNext();) {
        Map.Entry e = (Entry) i.next();
        StatsRecord r = (StatsRecord) e.getValue();
        printDetailsIfNecessary(r, total);
      }
      if (printTotal) printDetailsIfNecessary(total, null);
    }
  }

  public void run() {
    while (!isFinished()) {
      ThreadUtil.reallySleep(timeInterval);
      if (isFinished()) return;
      print();
    }
  }

  public void printDetailsIfNecessary(StatsRecord toPrint, StatsRecord total) {
    long[] counters = toPrint.getAllAndReset();
    if (counters.length != 0 && counters[0] != 0) {
      if (printHeader) {
        statsLogger.info(header);
        statsLogger.info("==========================================================");
        printHeader = false;
      }
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
    private final String               key;
    private final CopyOnWriteArrayList counterList = new CopyOnWriteArrayList();

    public StatsRecord(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }

    public void update(long[] counters) {
      if (counters.length > counterList.size()) {
        initCounters(counters.length);
      }
      for (int i = 0; i < counters.length; i++) {
        ((SynchronizedLong) counterList.get(i)).add(counters[i]);
      }
    }

    private void initCounters(int num) {
      while (counterList.size() < num) {
        counterList.add(new SynchronizedLong(0));
      }
    }

    public long[] getAllAndReset() {
      long ret[] = new long[counterList.size()];
      int size = counterList.size();
      for (int idx = 0; idx < size; idx++) {
        ret[idx] = ((SynchronizedLong) counterList.get(idx)).set(0);
      }
      return ret;
    }
  }

  private static class Aggregator implements Runnable {
    private final List printers     = new ArrayList();
    public static long timeInterval = TCPropertiesImpl.getProperties()
                                        .getLong(TCPropertiesConsts.STATS_PRINTER_INTERVAL);
    private Thread     thread;

    void addStatsPrinter(StatsPrinter printer) {
      printers.add(printer);
      if (thread == null || !thread.isAlive()) {
        thread = new Thread(this, "Stats Printer Aggregator");
        thread.setDaemon(true);
        thread.start();
      }
    }

    public void run() {
      while (true) {
        if (printers.size() == 0) {
          return;
        } else {
          ThreadUtil.reallySleep(timeInterval);
        }
        Iterator iter = printers.iterator();
        while (iter.hasNext()) {
          StatsPrinter printer = (StatsPrinter) iter.next();
          if (printer.isFinished()) {
            iter.remove();
          } else {
            printer.print();
          }
        }
      }
    }

  }
}
