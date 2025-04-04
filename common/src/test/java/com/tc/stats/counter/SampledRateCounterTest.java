/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.stats.counter;


import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.stats.counter.sampled.derived.SampledRateCounterConfig;
import com.tc.util.Assert;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class SampledRateCounterTest extends TestCase {

  public void testInitialValue() {

    SampledRateCounterConfig config = new SampledRateCounterConfig(1, 300, true);

    Counter counter = config.createCounter();
    assertEquals(0L, counter.getValue());

    config = new SampledRateCounterConfig(1, 300, true, 42, 21);
    counter = config.createCounter();
    assertEquals(2L, counter.getValue());
  }

  public void testUnsupportedOperations() {
    SampledRateCounterConfig config = new SampledRateCounterConfig(1, 300, true);

    Counter counter = config.createCounter();

    Exception nullException = null;

    try {
      counter.getAndSet(0);
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

    try {
      counter.setValue(0);
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

    try {
      counter.decrement();
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

    try {
      counter.decrement(0);
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

    try {
      counter.increment();
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

    try {
      counter.increment(0);
    } catch (UnsupportedOperationException e) {
      // expected
      nullException = e;
    }
    if (nullException == null) {
      Assert.fail("Should be unsupported");
    }

  }

  public void testConcurrency() throws InterruptedException {
    SampledRateCounterConfig config = new SampledRateCounterConfig(1, 300, false);

    final SampledRateCounter counter = (SampledRateCounter) config.createCounter();

    final AtomicLong localNumerator = new AtomicLong(0L);
    final AtomicLong localDenominator = new AtomicLong(0L);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);

    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread() {
        @Override
        public void run() {
          try {
            Random random = new Random(hashCode());
            for (int n = 0; n < 100000; n++) {
              long numeratorOperand = getOperand(random);
              long denominatorOperand = getOperand(random);
              localNumerator.addAndGet(numeratorOperand);
              localDenominator.addAndGet(denominatorOperand);
              counter.increment(numeratorOperand, denominatorOperand);

              numeratorOperand = getOperand(random);
              denominatorOperand = getOperand(random);
              localNumerator.addAndGet(-numeratorOperand);
              localDenominator.addAndGet(-denominatorOperand);
              counter.decrement(numeratorOperand, denominatorOperand);

            }
          } catch (Throwable t) {
            t.printStackTrace();
            error.set(t);
          }
        }

        private long getOperand(Random random) {
          int operation = random.nextInt(2);
          long rv = 0;
          switch (operation) {
            case 0: {
              long amount = Math.abs(random.nextInt(100));
              rv = -1 * amount;
              break;
            }
            case 1: {
              rv = Math.abs(random.nextInt(100));
              break;
            }
            default: {
              throw new RuntimeException("operation " + operation);
            }
          }
          return rv;
        }
      };
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    if (error.get() != null) {
      fail(error.get().toString());
    }

    long localVal = localDenominator.get() == 0 ? 0 : (localNumerator.get() / localDenominator.get());
    System.out.println("localValue=" + localVal + " counter.getValue()=" + counter.getValue());
    assertEquals(localVal, counter.getValue());
  }

  public void test() {
    SampledRateCounterConfig config = new SampledRateCounterConfig(1, 300, false);
    final SampledRateCounter counter = (SampledRateCounter) config.createCounter();

    assertEquals(0L, counter.getValue());

    counter.decrement(1, -1);
    assertEquals(-1L, counter.getValue());

    counter.increment(1, -1);
    assertEquals(0L, counter.getValue());

    counter.decrement(10L, -1);
    assertEquals(-10L, counter.getValue());

    counter.increment(10L, -1);
    assertEquals(0L, counter.getValue());

    counter.increment(-10L, 1);
    assertEquals(-10L, counter.getValue());

    counter.decrement(-10L, 1);
    assertEquals(0L, counter.getValue());

    counter.setValue(Long.MAX_VALUE, 1);
    assertEquals(Long.MAX_VALUE, counter.getValue());

    counter.setValue(Long.MIN_VALUE, 1);
    assertEquals(Long.MIN_VALUE, counter.getValue());

    counter.setValue(Long.MAX_VALUE, Long.MAX_VALUE);
    assertEquals(1, counter.getValue());

    counter.setValue(Long.MIN_VALUE, Long.MIN_VALUE);
    assertEquals(1, counter.getValue());

    counter.setValue(0L, 0L);
    assertEquals(0L, counter.getValue());

    counter.increment(1, 1);
    counter.increment(1, 0);
    counter.increment(1, 0);
    long value = counter.getAndReset();
    assertEquals(3L, value);
    assertEquals(0L, counter.getValue());

    counter.setNumeratorValue(0);
    counter.setDenominatorValue(0);
    assertEquals(0L, counter.getValue());

    counter.setNumeratorValue(1);
    counter.setDenominatorValue(0);
    assertEquals(0L, counter.getValue());

    counter.setNumeratorValue(0);
    counter.setDenominatorValue(1);
    assertEquals(0L, counter.getValue());

    counter.setNumeratorValue(10);
    counter.setDenominatorValue(2);
    assertEquals(5L, counter.getValue());

    counter.setNumeratorValue(-10);
    counter.setDenominatorValue(2);
    assertEquals(-5L, counter.getValue());

    counter.setNumeratorValue(10);
    counter.setDenominatorValue(-2);
    assertEquals(-5L, counter.getValue());

    counter.setNumeratorValue(-10);
    counter.setDenominatorValue(-2);
    assertEquals(5L, counter.getValue());
  }

}
