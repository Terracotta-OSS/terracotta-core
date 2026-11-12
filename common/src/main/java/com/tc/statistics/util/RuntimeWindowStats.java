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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;

/**
 * Tracks the runtime of events over a configurable set of sliding windows (by default the last 30 seconds, 1 minute,
 * 10 minutes, 1 hour and 1 day) and over the total lifetime of the instance.
 * <p>
 * Each call to {@link #record(long)} supplies the runtime of one event; the time of the event is taken from the
 * clock at the moment of the call. The runtime unit is opaque to this class (nanoseconds, milliseconds...), the
 * caller decides and the same unit is reported back.
 * <p>
 * This class is optimized for frequent writes and rare reads and is lock free. Time is divided into fixed width
 * buckets held in rings, one ring per {@link Level} (by default 10s, 1m and 5m buckets). A bucket accumulates count
 * and sum in {@link LongAdder}s and min and max in {@link LongAccumulator}s, which spread contended updates across
 * cells.
 * <p>
 * A write updates only the current bucket of the finest level. Everything else is deferred. When a write needs a ring
 * slot that still holds an older bucket, the old bucket is replaced with a single CAS and its totals are folded into the
 * next level (a 10s bucket becomes part of a 1m bucket, and so on). This happens once per slot per ring cycle, not per
 * event. Reads walk the rings from the finest level up to the level backing the requested window and merge the buckets
 * which overlap the window. A bucket lives in exactly one ring at a time, so nothing is counted twice. The total is
 * likewise computed on read: everything retired out of the finest ring plus the buckets still in it.
 * <p>
 * The results are approximate by design:
 * <ul>
 * <li>A window may include events up to one bucket width older than the window (the width of the level backing it).
 * It never leaves out events inside the window.</li>
 * <li>Reads are not an atomic cut: an event recorded concurrently with a read may be partially visible (for example
 * included in the count but not yet in the sum), and a bucket in the middle of being retired may be momentarily
 * missed.</li>
 * <li>A writer which is delayed between choosing a bucket and updating it for longer than the time it takes to retire
 * that bucket may lose its event. This can affect the total as well as the windows.</li>
 * </ul>
 */
public class RuntimeWindowStats {

  /**
   * A level is a ring of fixed width time buckets.
   */
  public static final class Level {
    private final long widthNanos;
    private final int buckets;

    /**
     * @param bucketWidth the time covered by each bucket
     * @param buckets the number of buckets in the ring, at least 2. A window can be served by this level if it is no
     *        longer than {@code (buckets - 1) * bucketWidth}
     */
    public Level(Duration bucketWidth, int buckets) {
      if (bucketWidth.isZero() || bucketWidth.isNegative()) {
        throw new IllegalArgumentException("bucket width must be positive: " + bucketWidth);
      }
      if (buckets < 2) {
        throw new IllegalArgumentException("a level needs at least 2 buckets: " + buckets);
      }
      this.widthNanos = bucketWidth.toNanos();
      this.buckets = buckets;
    }

    private long coverageNanos() {
      return widthNanos * (buckets - 1);
    }
  }

  public static final List<Level> DEFAULT_LEVELS = Collections.unmodifiableList(Arrays.asList(
      new Level(Duration.ofSeconds(10), 64),
      new Level(Duration.ofMinutes(1), 64),
      new Level(Duration.ofMinutes(5), 300)));

  public static final List<Duration> DEFAULT_WINDOWS = Collections.unmodifiableList(Arrays.asList(
      Duration.ofSeconds(30),
      Duration.ofMinutes(1),
      Duration.ofMinutes(10),
      Duration.ofHours(1),
      Duration.ofDays(1)));

  /**
   * Immutable view of the events seen in one window.
   */
  public static final class Snapshot {
    private final long count;
    private final long sum;
    private final long min;
    private final long max;

    Snapshot(long count, long sum, long min, long max) {
      this.count = count;
      this.sum = sum;
      this.min = count == 0 ? 0 : min;
      this.max = count == 0 ? 0 : max;
    }

    public long getCount() {
      return count;
    }

    public long getSum() {
      return sum;
    }

    public long getMin() {
      return min;
    }

    public long getMax() {
      return max;
    }

    public double getAverage() {
      return count == 0 ? 0d : (double) sum / count;
    }

    @Override
    public String toString() {
      return "count=" + count + ", sum=" + sum + ", min=" + min + ", max=" + max + ", avg=" + getAverage();
    }
  }

  /**
   * Lock free accumulator of count, sum, min and max. Min and max are written first and count last so that a
   * concurrent reader which sees a count also sees (at least) the matching min and max.
   */
  private static class Stats {
    final LongAdder count = new LongAdder();
    final LongAdder sum = new LongAdder();
    final LongAccumulator min = new LongAccumulator(Math::min, Long.MAX_VALUE);
    final LongAccumulator max = new LongAccumulator(Math::max, Long.MIN_VALUE);

    void add(long runtime) {
      merge(1, runtime, runtime, runtime);
    }

    void merge(long c, long s, long mn, long mx) {
      min.accumulate(mn);
      max.accumulate(mx);
      sum.add(s);
      count.add(c);
    }
  }

  private static final class Bucket extends Stats {
    private final long index;

    Bucket(long index) {
      this.index = index;
    }
  }

  private static final class Merge {
    long count;
    long sum;
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    void add(Stats s) {
      // count is read first, matching the order in which Stats.merge writes it last
      count += s.count.sum();
      sum += s.sum.sum();
      min = Math.min(min, s.min.get());
      max = Math.max(max, s.max.get());
    }

    Snapshot snapshot() {
      return new Snapshot(count, sum, min, max);
    }
  }

  private static final class WindowSpec {
    private final long durationNanos;
    private final int level;

    WindowSpec(long durationNanos, int level) {
      this.durationNanos = durationNanos;
      this.level = level;
    }
  }

  private final LongSupplier nanoClock;
  private final long[] width;
  private final int[] size;
  private final AtomicReferenceArray<Bucket>[] rings;
  private final Map<Duration, WindowSpec> windows = new LinkedHashMap<>();
  // everything which has been retired out of the finest ring
  private final Stats retired = new Stats();

  public RuntimeWindowStats() {
    this(System::nanoTime);
  }

  /**
   * @param nanoClock monotonic clock in nanoseconds, injectable for testing
   */
  public RuntimeWindowStats(LongSupplier nanoClock) {
    this(nanoClock, DEFAULT_LEVELS, DEFAULT_WINDOWS);
  }

  /**
   * @param levels the rings of buckets, finest first. Each bucket width must be a larger exact multiple of the
   *        previous one
   * @param windows the windows to report. Each is served by the finest level which can cover it, and must fit within
   *        the coverage of the coarsest level
   * @throws IllegalArgumentException if the levels or windows are not valid
   */
  public RuntimeWindowStats(List<Level> levels, Collection<Duration> windows) {
    this(System::nanoTime, levels, windows);
  }

  /**
   * @param nanoClock monotonic clock in nanoseconds, injectable for testing
   * @see #RuntimeWindowStats(List, Collection)
   */
  @SuppressWarnings("unchecked")
  public RuntimeWindowStats(LongSupplier nanoClock, List<Level> levels, Collection<Duration> windows) {
    if (levels.isEmpty()) {
      throw new IllegalArgumentException("at least one level is required");
    }
    this.nanoClock = nanoClock;
    int n = levels.size();
    this.width = new long[n];
    this.size = new int[n];
    this.rings = new AtomicReferenceArray[n];
    for (int l = 0; l < n; l++) {
      Level level = levels.get(l);
      if (l > 0 && (level.widthNanos <= width[l - 1] || level.widthNanos % width[l - 1] != 0)) {
        throw new IllegalArgumentException("level " + l + " bucket width must be a larger multiple of level " + (l - 1));
      }
      width[l] = level.widthNanos;
      size[l] = level.buckets;
      rings[l] = new AtomicReferenceArray<>(level.buckets);
    }
    for (Duration d : windows) {
      if (d.isZero() || d.isNegative()) {
        throw new IllegalArgumentException("window must be positive: " + d);
      }
      long nanos = d.toNanos();
      int found = -1;
      for (int l = 0; l < n && found < 0; l++) {
        if (levels.get(l).coverageNanos() >= nanos) {
          found = l;
        }
      }
      if (found < 0) {
        throw new IllegalArgumentException("window " + d + " is longer than the coverage of the coarsest level");
      }
      if (this.windows.put(d, new WindowSpec(nanos, found)) != null) {
        throw new IllegalArgumentException("duplicate window: " + d);
      }
    }
  }

  /**
   * Record the runtime of an event which has just completed.
   *
   * @param runtime the runtime of the event, in the caller's chosen unit
   */
  public void record(long runtime) {
    long idx = Math.floorDiv(nanoClock.getAsLong(), width[0]);
    Bucket b = bucket(0, idx);
    if (b != null) {
      b.add(runtime);
    } else {
      // so late that the slot already holds a newer bucket, put it in the first level which will take it
      retired.add(runtime);
      if (rings.length > 1) {
        fold(1, parent(0, idx), 1, runtime, runtime, runtime);
      }
    }
  }

  /**
   * Find the bucket for the index. A bucket occupying the slot which is older is retired and replaced.
   *
   * @return the bucket, or null if the slot already holds a newer bucket (the event is too old for this level)
   */
  private Bucket bucket(int level, long idx) {
    AtomicReferenceArray<Bucket> ring = rings[level];
    int slot = (int) Math.floorMod(idx, (long) size[level]);
    Bucket created = null;
    for (;;) {
      Bucket cur = ring.get(slot);
      if (cur != null) {
        if (cur.index == idx) {
          return cur;
        } else if (cur.index > idx) {
          return null;
        }
      }
      if (created == null) {
        created = new Bucket(idx);
      }
      if (ring.compareAndSet(slot, cur, created)) {
        if (cur != null) {
          retire(level, cur);
        }
        return created;
      }
    }
  }

  /**
   * Move the totals of a bucket which has left its ring into the next level. Once it has left the coarsest level it is
   * older than any window and is dropped.
   */
  private void retire(int level, Bucket old) {
    long c = old.count.sum();
    long s = old.sum.sum();
    long mn = old.min.get();
    long mx = old.max.get();
    if (level == 0) {
      retired.merge(c, s, mn, mx);
    }
    if (level + 1 < rings.length) {
      fold(level + 1, parent(level, old.index), c, s, mn, mx);
    }
  }

  /**
   * Merge into the bucket at the level, or if that is already too old for the level, the first coarser level
   * which will take it.
   */
  private void fold(int level, long idx, long c, long s, long mn, long mx) {
    for (; level < rings.length; level++) {
      Bucket b = bucket(level, idx);
      if (b != null) {
        b.merge(c, s, mn, mx);
        return;
      }
      if (level + 1 < rings.length) {
        idx = parent(level, idx);
      }
    }
  }

  private long parent(int level, long idx) {
    return Math.floorDiv(idx * width[level], width[level + 1]);
  }

  /**
   * @return the windows being reported, in the order configured
   */
  public Collection<Duration> getWindows() {
    return Collections.unmodifiableSet(windows.keySet());
  }

  /**
   * @param window one of the configured windows
   * @throws IllegalArgumentException if the window is not configured
   */
  public Snapshot get(Duration window) {
    WindowSpec spec = windows.get(window);
    if (spec == null) {
      throw new IllegalArgumentException("window not configured: " + window + ", configured: "
          + new ArrayList<>(windows.keySet()));
    }
    return get(spec, nanoClock.getAsLong());
  }

  /**
   * @return the events seen over the lifetime of the instance
   */
  public Snapshot getTotal() {
    Merge merge = new Merge();
    merge.add(retired);
    AtomicReferenceArray<Bucket> ring = rings[0];
    for (int i = 0; i < size[0]; i++) {
      Bucket b = ring.get(i);
      if (b != null) {
        merge.add(b);
      }
    }
    return merge.snapshot();
  }

  /**
   * @return a snapshot of every configured window, all evaluated against the same instant, in the order configured
   */
  public Map<Duration, Snapshot> getAll() {
    long now = nanoClock.getAsLong();
    Map<Duration, Snapshot> result = new LinkedHashMap<>();
    for (Map.Entry<Duration, WindowSpec> e : windows.entrySet()) {
      result.put(e.getKey(), get(e.getValue(), now));
    }
    return result;
  }

  private Snapshot get(WindowSpec window, long now) {
    Merge merge = new Merge();
    long cutoff = now - window.durationNanos;
    for (int l = 0; l <= window.level; l++) {
      AtomicReferenceArray<Bucket> ring = rings[l];
      for (int i = 0; i < size[l]; i++) {
        Bucket b = ring.get(i);
        // include any bucket which overlaps the window
        if (b != null && (b.index + 1) * width[l] > cutoff) {
          merge.add(b);
        }
      }
    }
    return merge.snapshot();
  }

  public Map<String, ?> convert() {
    Map<String, Object> values = new LinkedHashMap<>();
    for (Duration d : getWindows()) {
      Map<String, Object> seg = new HashMap<>();
      Snapshot ss = get(d);
      seg.put("average", ss.getAverage());
      seg.put("count", ss.getCount());
      seg.put("max", ss.getMax());
      seg.put("min", ss.getMin());
      values.put(d.toString(), seg);
    }
    return values;
  }
}
