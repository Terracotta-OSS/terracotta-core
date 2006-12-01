/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.SerializationUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

final class StatsCollector implements Serializable {

  private List stats;
  private int  success = 0;
  private int  errors  = 0;

  public StatsCollector() {
    this.stats = new LinkedList();
  }

  void addStat(ResponseStatistic stat) {
    synchronized (stats) {
      stats.add(stat);

      boolean isSuccess = stat.statusCode() == HttpStatus.SC_OK;
      if (isSuccess) {
        success++;
      } else {
        errors++;
      }

      int size = stats.size();
      if ((size % 500) == 0) {
        System.out.println("Completed " + size + " requests (" + success + " OK, " + errors + " errors)");
      }
    }
  }

  public static StatsCollector read(InputStream in) {
    return (StatsCollector) SerializationUtils.deserialize(in);
  }

  void write(OutputStream out) {
    SerializationUtils.serialize(this, out);
  }

  public void add(StatsCollector sc) {
    ResponseStatistic rss[] = sc.toArray();
    synchronized (stats) {
      for (int i = 0; i < rss.length; i++) {
        stats.add(rss[i]);
      }
    }
  }

  public ResponseStatistic[] toArray() {
    synchronized (stats) {
      return (ResponseStatistic[]) stats.toArray(new ResponseStatistic[0]);
    }
  }
}
