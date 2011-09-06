/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics.util;

public interface StatsRecorder {
  public static final long[] SINGLE_INCR = new long[] { 1 };

  public void updateStats(String key, long[] counters);

  public void finish();
}
