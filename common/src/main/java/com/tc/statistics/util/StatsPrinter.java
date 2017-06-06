/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.statistics.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class StatsPrinter implements StatsRecorder, Runnable {
  private static final Aggregator aggregator   = new Aggregator();

  private final static Logger statsLogger  = LoggerFactory.getLogger(StatsPrinter.class);
  private final long              timeInterval;
  private final ConcurrentMap<String, StatsRecord> statsRecords = new ConcurrentHashMap<String, StatsRecord>();
  private final MessageFormat     formatLine;
  private final String            header;
  private volatile int            keyMaxSize   = 0;
  private boolean                 printHeader;
  private final boolean           printTotal;
  private volatile boolean        finished     = false;

  public StatsPrinter(String name, long timeInterval, MessageFormat header, MessageFormat formatLine, boolean printTotal) {
    this.printTotal = printTotal;
    this.timeInterval = timeInterval;
    this.header = header.format(new Long[] { Long.valueOf(timeInterval) });
    this.formatLine = formatLine;

    Thread t = new Thread(this, name);
    t.setDaemon(true);
    t.start();
  }

  public StatsPrinter(MessageFormat header, MessageFormat formatLine, boolean printTotal) {
    this.printTotal = printTotal;
    this.timeInterval = -1;
    this.header = header.format(new Long[] { Long.valueOf(Aggregator.timeInterval) });
    this.formatLine = formatLine;

    aggregator.addStatsPrinter(this);
  }

  @Override
  public void finish() {
    this.finished = true;
  }

  private boolean isFinished() {
    return finished;
  }

  @Override
  public void updateStats(String key, long[] counters) {
    StatsRecord r = get(key);
    r.update(counters);
  }

  private StatsRecord get(String key) {
    StatsRecord r = statsRecords.get(key);
    if (r == null) {
      r = new StatsRecord(key);
      StatsRecord old = statsRecords.putIfAbsent(key, r);
      if (old != null) {
        r = old;
      }
      updateKeyMaxSize(key.length());
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
      for (StatsRecord sr : statsRecords.values()) {
        printDetailsIfNecessary(sr, total);
      }
      if (printTotal) printDetailsIfNecessary(total, null);
    }
  }

  @Override
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
    private final CopyOnWriteArrayList<AtomicLong> counterList = new CopyOnWriteArrayList<AtomicLong>();

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
        counterList.get(i).addAndGet(counters[i]);
      }
    }

    private void initCounters(int num) {
      while (counterList.size() < num) {
        counterList.add(new AtomicLong(0));
      }
    }

    public long[] getAllAndReset() {
      long ret[] = new long[counterList.size()];
      int size = counterList.size();
      for (int idx = 0; idx < size; idx++) {
        ret[idx] = counterList.get(idx).getAndSet(0);
      }
      return ret;
    }
  }

  private static class Aggregator implements Runnable {
    private final List<StatsPrinter> printers = new ArrayList<StatsPrinter>();
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

    @Override
    public void run() {
      while (true) {
        if (printers.size() == 0) {
          return;
        } else {
          ThreadUtil.reallySleep(timeInterval);
        }
        Iterator<StatsPrinter> iter = printers.iterator();
        while (iter.hasNext()) {
          StatsPrinter printer = iter.next();
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
