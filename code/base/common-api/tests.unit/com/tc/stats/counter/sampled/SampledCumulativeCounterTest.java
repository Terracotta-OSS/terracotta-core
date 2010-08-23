/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SampledCumulativeCounterTest extends TestCase {

  public static final Random rand = new Random(System.currentTimeMillis());

  public void testResetOnSampleCumulativeCounter() {
    System.out.println("Testing with resetOnSample: true");
    doTest(true);
    System.out.println("Testing with resetOnSample: false");
    doTest(false);
  }

  private void doTest(boolean resetOnSample) {
    CounterManager manager = new CounterManagerImpl();
    SampledCumulativeCounterConfig config = new SampledCumulativeCounterConfig(1, 300, resetOnSample, 0);
    SampledCumulativeCounter counter = (SampledCumulativeCounter) manager.createCounter(config);
    for (int i = 0; i < 5; i++) {
      // sleep between 3 to 4 secs, at least 3
      int sleep = rand.nextInt(1000) + 3000;
      System.out.println("Sleeping for " + sleep + " millis");
      ThreadUtil.reallySleep(sleep);
      // assert last value is always 0
      if (resetOnSample) {
        assertEquals(0, counter.getMostRecentSample().getCounterValue());
      }
      assertEquals(i, counter.getCumulativeValue());

      counter.increment();
    }
    manager.shutdown();
  }

  public void testContinuousIncDec() {
    CounterManager manager = new CounterManagerImpl();
    int initialValue = 5;
    SampledCumulativeCounterConfig config = new SampledCumulativeCounterConfig(1, 300, true, initialValue);
    SampledCumulativeCounter counter = (SampledCumulativeCounter) manager.createCounter(config);

    assertEquals(initialValue, counter.getCumulativeValue());

    int inc = rand.nextInt(100) + 100;
    int dec = inc - rand.nextInt(100);
    System.out.println("Testing continuous increment()");
    for (int i = 1; i <= inc; i++) {
      counter.increment();
      assertEquals(initialValue + i, counter.getCumulativeValue());
    }

    System.out.println("Testing continuous decrement()");
    for (int i = 1; i <= dec; i++) {
      counter.decrement();
      assertEquals(initialValue + inc - i, counter.getCumulativeValue());
    }
    assertEquals(initialValue + inc - dec, counter.getCumulativeValue());

    System.out.println("Testing continuous increment(amount)");
    initialValue = initialValue + inc - dec;
    int times = rand.nextInt(100) + 10;
    long currentValue = counter.getCumulativeValue();
    for (int i = 0; i < times; i++) {
      int amount = rand.nextInt(20) + 5;
      counter.increment(amount);
      currentValue += amount;
      assertEquals(currentValue, counter.getCumulativeValue());
    }

    System.out.println("Testing continuous decrement(amount)");
    times = rand.nextInt(100) + 10;
    currentValue = counter.getCumulativeValue();
    for (int i = 0; i < times; i++) {
      int amount = rand.nextInt(20) + 5;
      counter.decrement(amount);
      currentValue -= amount;
      assertEquals(currentValue, counter.getCumulativeValue());
    }

    manager.shutdown();
  }

  public static void assertEquals(long e, long a) {
    System.out.println("expected: " + e + " actual: " + a);
    Assert.assertEquals(e, a);
  }

}
