/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.util;

public class NullStatsRecorder implements StatsRecorder {

  public void updateStats(String key, long[] counters) {
    //NOP
  }

  public void finish() {
    /**/
  }
}
