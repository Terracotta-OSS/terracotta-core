/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectManagerStats;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.TimeStampedCounterValue;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements the object manager stats
 */
public class ObjectManagerStatsImpl implements ObjectManagerStatsListener, ObjectManagerStats {

  private final AtomicLong     cacheHits      = new AtomicLong();
  private final AtomicLong     cacheMisses    = new AtomicLong();
  private final AtomicLong     objectsCreated = new AtomicLong();
  private final SampledCounter newObjectCounter;
  private final SampledCounter faultRateCounter;
  private final SampledCounter flushRateCounter;

  public ObjectManagerStatsImpl(SampledCounter newObjectCounter, SampledCounter faultRateCounter,
                                SampledCounter flushRateCounter) {
    this.newObjectCounter = newObjectCounter;
    this.faultRateCounter = faultRateCounter;
    this.flushRateCounter = flushRateCounter;
  }

  public void cacheHit() {
    this.cacheHits.incrementAndGet();
  }

  public void cacheMiss() {
    this.cacheMisses.incrementAndGet();
    this.faultRateCounter.increment();
  }

  public void newObjectCreated() {
    this.objectsCreated.incrementAndGet();
    this.newObjectCounter.increment();
  }

  public double getCacheHitRatio() {
    return ((double) this.cacheHits.get()) / (getTotalRequests());
  }

  public long getTotalRequests() {
    return this.cacheHits.get() + this.cacheMisses.get();
  }

  public long getTotalCacheMisses() {
    return this.cacheMisses.get();
  }

  public long getTotalCacheHits() {
    return this.cacheHits.get();
  }

  public long getTotalObjectsCreated() {
    return this.objectsCreated.get();
  }

  public TimeStampedCounterValue getOnHeapFaultRate() {
    return this.faultRateCounter.getMostRecentSample();
  }

  public void flushed(int count) {
    this.flushRateCounter.increment(count);
  }

  public TimeStampedCounterValue getOnHeapFlushRate() {
    return this.flushRateCounter.getMostRecentSample();
  }
}
