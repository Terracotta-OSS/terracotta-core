/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval.actions;

import com.tc.stats.counter.Counter;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;

public class CounterIncrementer implements Runnable {
  private static final Random random = new Random(System.currentTimeMillis());
  private final Counter counter;
  private int intervalMillis;
  private boolean incrementCounter = true;
  final private int minDelta;
  final private int maxDelta;

  public CounterIncrementer(Counter counter, int intervalMillis) {
    this(counter, intervalMillis, 1, 10);
  }

  public CounterIncrementer(Counter counter, int intervalMillis, int maxDelta) {
    this(counter, intervalMillis, 1, maxDelta);
  }

  public CounterIncrementer(Counter counter, int intervalMillis, int minDelta, int maxDelta) {
    Assert.assertNotNull(counter);
    Assert.eval(intervalMillis > 0);
    Assert.eval(maxDelta > minDelta);
    this.counter = counter;
    this.intervalMillis = intervalMillis;
    this.minDelta = minDelta;
    this.maxDelta = maxDelta;
  }

  public void run() {
    while (incrementCounter) {
      int delta = getDelta();
      if (counter instanceof SampledRateCounter) {
        int delta2 = getDelta();
        ((SampledRateCounter) counter).increment(delta + delta2, delta);
      }
      else counter.increment(delta);
      Assert.eval(delta >= minDelta && delta < maxDelta);
      ThreadUtil.reallySleep(intervalMillis);
    }
  }

  private int getDelta() {
    return random.nextInt(maxDelta - minDelta) + minDelta;
  }

  public void stopCounterIncrement() {
    incrementCounter = false;
  }
}