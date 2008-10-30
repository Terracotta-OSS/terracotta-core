/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
