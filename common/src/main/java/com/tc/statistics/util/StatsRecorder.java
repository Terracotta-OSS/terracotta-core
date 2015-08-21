/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics.util;

public interface StatsRecorder {
  public static final long[] SINGLE_INCR = new long[] { 1 };

  public void updateStats(String key, long[] counters);

  public void finish();
}
