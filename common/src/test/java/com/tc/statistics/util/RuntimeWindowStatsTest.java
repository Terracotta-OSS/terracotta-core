/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
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
package com.tc.statistics.util;

import com.tc.statistics.util.RuntimeWindowStats.Level;
import com.tc.statistics.util.RuntimeWindowStats.Snapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuntimeWindowStatsTest {

  private static final Duration S30 = Duration.ofSeconds(30);
  private static final Duration MIN = Duration.ofMinutes(1);
  private static final Duration MIN10 = Duration.ofMinutes(10);
  private static final Duration HOUR = Duration.ofHours(1);
  private static final Duration DAY = Duration.ofDays(1);

  private final AtomicLong clock = new AtomicLong(TimeUnit.HOURS.toNanos(5));
  private final RuntimeWindowStats stats = new RuntimeWindowStats(clock::get);

  @Test
  public void testEmpty() {
    for (Duration w : stats.getWindows()) {
      assertEmpty(stats.get(w));
    }
    assertEmpty(stats.getTotal());
  }

  private static void assertEmpty(Snapshot s) {
    assertEquals(0, s.getCount());
    assertEquals(0, s.getMin());
    assertEquals(0, s.getMax());
    assertEquals(0d, s.getAverage(), 0d);
  }

  @Test
  public void testDefaultWindows() {
    assertEquals(Arrays.asList(S30, MIN, MIN10, HOUR, DAY), new ArrayList<>(stats.getWindows()));
    assertEquals(Arrays.asList(S30, MIN, MIN10, HOUR, DAY), new ArrayList<>(stats.getAll().keySet()));
  }

  @Test
  public void testAggregates() {
    stats.record(10);
    stats.record(30);
    stats.record(20);
    List<Snapshot> all = new ArrayList<>(stats.getAll().values());
    all.add(stats.getTotal());
    for (Snapshot s : all) {
      assertEquals(3, s.getCount());
      assertEquals(60, s.getSum());
      assertEquals(10, s.getMin());
      assertEquals(30, s.getMax());
      assertEquals(20d, s.getAverage(), 0d);
    }
  }

  @Test
  public void testEventsAgeOutOfWindows() {
    stats.record(100);

    clock.addAndGet(TimeUnit.SECONDS.toNanos(45));
    assertEquals(0, stats.get(S30).getCount());
    assertEquals(1, stats.get(MIN).getCount());

    clock.addAndGet(TimeUnit.SECONDS.toNanos(30));
    assertEquals(0, stats.get(MIN).getCount());
    assertEquals(1, stats.get(MIN10).getCount());

    clock.addAndGet(TimeUnit.MINUTES.toNanos(10));
    assertEquals(0, stats.get(MIN10).getCount());
    assertEquals(1, stats.get(HOUR).getCount());

    clock.addAndGet(TimeUnit.HOURS.toNanos(1));
    assertEquals(0, stats.get(HOUR).getCount());
    assertEquals(1, stats.get(DAY).getCount());

    clock.addAndGet(TimeUnit.DAYS.toNanos(1));
    assertEquals(0, stats.get(DAY).getCount());
    assertEquals(1, stats.getTotal().getCount());
    assertEquals(100, stats.getTotal().getSum());
  }

  @Test
  public void testSlotReuseStartsFreshBucket() {
    stats.record(5);
    clock.addAndGet(TimeUnit.SECONDS.toNanos(40));
    stats.record(7);
    Snapshot s = stats.get(S30);
    assertEquals(1, s.getCount());
    assertEquals(7, s.getMin());
    assertEquals(7, s.getMax());
    assertEquals(2, stats.get(MIN).getCount());
  }

  @Test
  public void testRetiringFinestBucketKeepsTotalAndLongerWindows() {
    stats.record(5);
    // 64 buckets of 10s later the same ring slot is needed, retiring the first bucket into the 1m level
    clock.addAndGet(TimeUnit.SECONDS.toNanos(640));
    stats.record(7);
    assertEquals(1, stats.get(MIN10).getCount());
    assertEquals(7, stats.get(MIN10).getSum());
    assertEquals(2, stats.get(HOUR).getCount());
    assertEquals(12, stats.get(HOUR).getSum());
    assertEquals(2, stats.get(DAY).getCount());
    assertEquals(2, stats.getTotal().getCount());
    assertEquals(5, stats.getTotal().getMin());
    assertEquals(7, stats.getTotal().getMax());
  }

  @Test
  public void testRollUpLosesNothing() {
    int seconds = 2 * 60 * 60;
    for (int i = 0; i < seconds; i++) {
      stats.record(1);
      clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
    }
    assertEquals(seconds, stats.getTotal().getCount());
    assertEquals(seconds, stats.get(DAY).getCount());
    // a window includes everything inside it and at most one bucket width more
    assertBetween(30, 40, stats.get(S30).getCount());
    assertBetween(60, 70, stats.get(MIN).getCount());
    assertBetween(600, 610, stats.get(MIN10).getCount());
    assertBetween(3600, 3660, stats.get(HOUR).getCount());
  }

  @Test
  public void testIdleGapThenRecord() {
    stats.record(1);
    clock.addAndGet(TimeUnit.HOURS.toNanos(3));
    stats.record(2);
    assertEquals(1, stats.get(HOUR).getCount());
    assertEquals(2, stats.get(DAY).getCount());
    assertEquals(2, stats.getTotal().getCount());

    clock.addAndGet(TimeUnit.DAYS.toNanos(2));
    stats.record(3);
    assertEquals(1, stats.get(DAY).getCount());
    assertEquals(3, stats.getTotal().getCount());
    assertEquals(6, stats.getTotal().getSum());
  }

  @Test
  public void testCustomLevelsAndWindows() {
    List<Level> levels = Arrays.asList(new Level(Duration.ofSeconds(1), 10), new Level(Duration.ofSeconds(10), 10));
    Duration five = Duration.ofSeconds(5);
    Duration eighty = Duration.ofSeconds(80);
    RuntimeWindowStats custom = new RuntimeWindowStats(clock::get, levels, Arrays.asList(eighty, five));

    assertEquals(Arrays.asList(eighty, five), new ArrayList<>(custom.getWindows()));
    for (int i = 0; i < 100; i++) {
      custom.record(2);
      clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
    }
    assertBetween(5, 6, custom.get(five).getCount());
    assertBetween(80, 90, custom.get(eighty).getCount());
    assertEquals(100, custom.getTotal().getCount());
    assertEquals(200, custom.getTotal().getSum());
  }

  @Test
  public void testSingleLevel() {
    Duration ten = Duration.ofSeconds(10);
    RuntimeWindowStats single = new RuntimeWindowStats(clock::get,
        Collections.singletonList(new Level(Duration.ofSeconds(1), 20)), Collections.singletonList(ten));
    for (int i = 0; i < 100; i++) {
      single.record(1);
      clock.addAndGet(TimeUnit.SECONDS.toNanos(1));
    }
    assertBetween(10, 11, single.get(ten).getCount());
    assertEquals(100, single.getTotal().getCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknownWindowRejected() {
    stats.get(Duration.ofSeconds(31));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWindowLongerThanCoverageRejected() {
    new RuntimeWindowStats(Collections.singletonList(new Level(Duration.ofSeconds(1), 10)),
        Collections.singletonList(Duration.ofSeconds(10)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLevelWidthMustBeMultiple() {
    new RuntimeWindowStats(Arrays.asList(new Level(Duration.ofSeconds(10), 10), new Level(Duration.ofSeconds(25), 10)),
        Collections.singletonList(Duration.ofSeconds(30)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoLevelsRejected() {
    new RuntimeWindowStats(Collections.<Level>emptyList(), Collections.singletonList(Duration.ofSeconds(1)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDuplicateWindowRejected() {
    new RuntimeWindowStats(RuntimeWindowStats.DEFAULT_LEVELS, Arrays.asList(MIN, MIN));
  }

  @Test
  public void testConcurrentWriters() throws Exception {
    int threads = 8;
    int perThread = 100_000;
    CountDownLatch start = new CountDownLatch(1);
    List<Thread> list = new ArrayList<>();
    for (int t = 0; t < threads; t++) {
      Thread th = new Thread(() -> {
        try {
          start.await();
        } catch (InterruptedException e) {
          return;
        }
        for (int i = 1; i <= perThread; i++) {
          stats.record(i);
        }
      });
      th.start();
      list.add(th);
    }
    start.countDown();
    for (Thread th : list) {
      th.join();
    }
    List<Snapshot> all = new ArrayList<>();
    for (Map.Entry<Duration, Snapshot> e : stats.getAll().entrySet()) {
      all.add(e.getValue());
    }
    all.add(stats.getTotal());
    for (Snapshot s : all) {
      assertEquals(threads * perThread, s.getCount());
      assertEquals(threads * (long) perThread * (perThread + 1) / 2, s.getSum());
      assertEquals(1, s.getMin());
      assertEquals(perThread, s.getMax());
    }
  }

  private static void assertBetween(long low, long high, long actual) {
    assertTrue("expected " + low + ".." + high + " but was " + actual, actual >= low && actual <= high);
  }
}
