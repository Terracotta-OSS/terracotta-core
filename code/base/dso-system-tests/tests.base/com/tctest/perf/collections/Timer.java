/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.perf.collections;

import com.tc.util.Assert;

public class Timer {

  private long startMillis;
  private long endMillis;
  private long elapsedMillis;
  private long maxElapsed;
  private long minElapsed;
  private long totalElapsed;
  private long lapseCount;

  public void start() {
    reset();
    startMillis = System.currentTimeMillis();
    endMillis = 0;
  }

  public void stop() {
    endMillis = System.currentTimeMillis();
    updateStats();
  }

  public void reset() {
    startMillis = 0;
    endMillis = 0;
  }

  public void restAll() {
    reset();
    maxElapsed = 0;
    minElapsed = 0;
    totalElapsed = 0;
    lapseCount = 0;
  }

  public long getElapsedMillis() {
    return elapsedMillis;
  }

  public long getLapseCount() {
    return lapseCount;
  }

  public long getMaxElapsed() {
    return maxElapsed;
  }

  public long getMinElapsed() {
    return minElapsed;
  }

  public long getTotalElapsed() {
    return totalElapsed;
  }

  public long getAvgElapsed() {
    if (lapseCount == 0) {
      Assert.inv(totalElapsed == 0);
      return 0;
    }
    return totalElapsed / lapseCount;
  }

  private void updateStats() {
    elapsedMillis = endMillis - startMillis;
    maxElapsed = Math.max(maxElapsed, elapsedMillis);
    minElapsed = Math.min(minElapsed, elapsedMillis);
    totalElapsed += elapsedMillis;
    lapseCount++;
  }

}
