/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.SerializationUtils;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import java.io.InputStream;
import java.io.Serializable;

final class StatsCollector implements Serializable {

  private int               count;
  private int               success = 0;
  private int               errors  = 0;
  private final LinkedQueue stats   = new LinkedQueue();
  private final Object      END     = new Object();

  public StatsCollector() {
    // ;
  }

  void addStat(ResponseStatistic stat) {
    synchronized (this) {
      count++;

      boolean isSuccess = stat.statusCode() == HttpStatus.SC_OK;
      if (isSuccess) {
        success++;
      } else {
        errors++;
      }

      if ((count % 500) == 0) {
        System.out.println("Completed " + count + " requests (" + success + " OK, " + errors + " errors)");
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
