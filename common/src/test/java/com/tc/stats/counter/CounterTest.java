/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats.counter;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class CounterTest extends TestCase {

  public void testInitialValue() {
    Counter counter = new CounterImpl();
    assertEquals(0L, counter.getValue());

    counter = new CounterImpl(42L);
    assertEquals(42L, counter.getValue());
  }

  public void testConcurrency() throws InterruptedException {
    final Counter counter = new CounterImpl();
    final AtomicLong local = new AtomicLong(0L);
    final AtomicReference<Throwable> error = new AtomicReference(null);

    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread() {
        @Override
        public void run() {
          try {
            Random random = new Random(hashCode());
            for (int n = 0; n < 100000; n++) {
              int operation = random.nextInt(4);

              switch (operation) {
                case 0: {
                  local.decrementAndGet();
                  counter.decrement();
                  break;
                }
                case 1: {
                  local.incrementAndGet();
                  counter.increment();
                  break;
                }
                case 2: {
                  long amount = random.nextLong();
                  local.addAndGet(-amount);
                  counter.decrement(amount);
                  break;
                }
                case 3: {
                  long amount = random.nextLong();
                  local.addAndGet(amount);
                  counter.increment(amount);
                  break;
                }
                default: {
                  throw new RuntimeException("operation " + operation);
                }
              }
            }
          } catch (Throwable t) {
            t.printStackTrace();
            error.set(t);
          }
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

    assertEquals(local.get(), counter.getValue());
  }
  
  public void test() {
    Counter counter = new CounterImpl();
    assertEquals(0L, counter.getValue());

    counter.decrement();
    assertEquals(-1L, counter.getValue());

    counter.increment();
    assertEquals(0L, counter.getValue());

    counter.decrement(10L);
    assertEquals(-10L, counter.getValue());

    counter.increment(10L);
    assertEquals(0L, counter.getValue());

    counter.increment(-10L);
    assertEquals(-10L, counter.getValue());

    counter.decrement(-10L);
    assertEquals(0L, counter.getValue());

    counter.setValue(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, counter.getValue());

    counter.setValue(Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, counter.getValue());

    counter.setValue(0L);
    assertEquals(0L, counter.getValue());

    counter.increment();
    counter.increment();
    counter.increment();
    long value = counter.getAndSet(42L);
    assertEquals(3L, value);
    assertEquals(42L, counter.getValue());
  }

}
