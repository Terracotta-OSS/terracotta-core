/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.SerializationUtils;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import java.io.InputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

final class StatsCollector implements Serializable {

  private static final long      REPORT        = 500;

  private int                    count;
  private int                    success       = 0;
  private int                    errors        = 0;
  private final LinkedQueue      stats         = new LinkedQueue();
  private final Object           END           = new Object();
  private long                   lastStartTime = -1;
  private long                   firstStat     = -1;
  private final NumberFormat     format        = NumberFormat.getInstance();
  private final SimpleDateFormat dateFormat    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

  public StatsCollector() {
    format.setMaximumFractionDigits(2);
  }

  void addStat(ResponseStatistic stat) {

    synchronized (this) {
      if (firstStat == -1) {
        firstStat = System.currentTimeMillis();
      }

      count++;

      boolean isSuccess = stat.statusCode() == HttpStatus.SC_OK;
      if (isSuccess) {
        success++;
      } else {
        errors++;
      }

      if ((count % REPORT) == 0) {
        final long start = lastStartTime == -1 ? firstStat : lastStartTime;
        final long end = System.currentTimeMillis();

        if (end < start) {
          System.out.println("\n**********************\nWARN: backward clock movement detected (" + end + " < " + start
                             + ")\n**********************\n");
        }

        final double rate = ((double) (REPORT * 1000L)) / ((double) (end - start));
        final double overallRate = ((double) (count * 1000L)) / ((double) (end - firstStat));

        System.out.println(dateFormat.format(new Date()) + ": Completed " + count + " requests (" + success + " OK, "
                           + errors + " errors), " + format.format(rate) + " tps, overall "
                           + format.format(overallRate) + " tps");

        lastStartTime = end;
      }
    }

    try {
      stats.put(stat);
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static StatsCollector read(InputStream in) {
    return (StatsCollector) SerializationUtils.deserialize(in);
  }

  public ResponseStatistic takeStat() {
    try {
      Object o = stats.take();
      if (o == END) { return null; }
      return (ResponseStatistic) o;
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public void finalStat() {
    try {
      stats.put(END);
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
