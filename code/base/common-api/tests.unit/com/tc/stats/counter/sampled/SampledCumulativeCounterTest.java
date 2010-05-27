/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter.sampled;

import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Random;

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
      // sleep between 2 to 3 secs, at least 2
      int sleep = rand.nextInt(1000) + 2000;
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

}
