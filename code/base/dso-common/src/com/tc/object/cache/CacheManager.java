/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.runtime.MemoryEventType;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.List;

public class CacheManager implements MemoryEventsListener {

  private static final TCLogger     logger              = TCLogging.getLogger(CacheManager.class);

  private static final State        INIT                = new State("INIT");
  private static final State        PROCESSING          = new State("PROCESSING");
  private static final State        COMPLETE            = new State("COMPLETE");

  private final Evictable           evictable;
  private final CacheConfig         config;
  private final TCMemoryManagerImpl memoryManager;

  private int                       calculatedCacheSize = 0;
  private CacheStatistics           lastStat            = null;

  public CacheManager(Evictable evictable, CacheConfig config) {
    this.evictable = evictable;
    this.config = config;
    this.memoryManager = new TCMemoryManagerImpl(config.getUsedThreshold(), config.getUsedCriticalThreshold(), config
        .getSleepInterval(), config.getLeastCount(), config.isOnlyOldGenMonitored());
    this.memoryManager.registerForMemoryEvents(this);
  }

  public void memoryUsed(MemoryEventType type, MemoryUsage usage) {
    CacheStatistics cp = new CacheStatistics(type, usage);
    evictable.evictCache(cp);
    cp.validate();
    addLastStat(cp);
  }

  // currently we only maintain 1 last stat
  private void addLastStat(CacheStatistics cp) {
    this.lastStat = cp;
  }

  private final class CacheStatistics implements CacheStats {
    private final MemoryEventType type;
    private final MemoryUsage     usage;

    private int                   countBefore;
    private int                   countAfter;
    private int                   evicted;
    private boolean               objectsGCed = false;
    private int                   toEvict;
    private long                  startTime;
    private State                 state       = INIT;

    public CacheStatistics(MemoryEventType type, MemoryUsage usage) {
      this.type = type;
      this.usage = usage;
    }

    public void validate() {
      if (state == PROCESSING) {
        // This might be ignored by the memory manager thread. TODO:: exit VM !!!
        throw new AssertionError(this + " : Object Evicted is not called. This indicates a bug in the software !");
      }
    }

    public int getObjectCountToEvict(int currentCount) {
      startTime = System.currentTimeMillis();
      countBefore = currentCount;
      adjustCachedObjectCount(currentCount);
      toEvict = computeObjects2Evict(currentCount);
      if (toEvict < 0 || toEvict > currentCount) {
        //
        throw new AssertionError("Computed Object to evict is out of range : toEvict = " + toEvict + " currentCount = "
                                 + currentCount + " " + this);
      }
      if (toEvict > 0) {
        state = PROCESSING;
      }
      if (config.isLoggingEnabled()) {
        logger.info("Asking to evict " + toEvict + " current size = " + currentCount + " calculated cache size = "
                    + calculatedCacheSize + " heap used = " + usage.getUsedPercentage() + " %  gc count = "
                    + usage.getCollectionCount());
      }
      return this.toEvict;
    }

    private void adjustCachedObjectCount(int currentCount) {
      if (type == MemoryEventType.BELOW_THRESHOLD) {
        int used = usage.getUsedPercentage();
        int diff = config.getUsedThreshold() - used;
        Assert.assertTrue(diff >= 0);
        calculatedCacheSize = currentCount + (currentCount * diff / 100);
      } else if (lastStat == null || lastStat.usage.getCollectionCount() < usage.getCollectionCount()) {
        // 1) This is the first threshold crossing alarm or
        // 2) A GC has taken place since the last time, but the memory has not gone below the threshold. (danger)
        int used = usage.getUsedPercentage();
        int diff = used - config.getUsedThreshold();
        Assert.assertTrue(diff >= 0);
        calculatedCacheSize = currentCount - (currentCount * diff / 100);
      }
    }

    public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC) {
      this.evicted = evictedCount;
      this.countAfter = currentCount;
      state = COMPLETE;
      // TODO:: add reference queue
      if (config.isLoggingEnabled()) {
        logger.info("Evicted " + evictedCount + " current Size = " + currentCount + " new objects created = "
                    + getNewObjectsCount() + " time taken = " + (System.currentTimeMillis() - startTime) + " ms");
      }
    }

    private int getNewObjectsCount() {
      return countAfter - (countBefore - evicted);
    }

    // TODO:: This need to be more intellegent. It should also check if a GC actually happened after eviction. Use
    // Reference Queue
    private int computeObjects2Evict(int currentCount) {
      if (type == MemoryEventType.BELOW_THRESHOLD || calculatedCacheSize > currentCount) { return 0; }
      int overshoot = currentCount - calculatedCacheSize;
      if (overshoot <= 0) return 0;
      int objects2Evict = overshoot + ((calculatedCacheSize * config.getPercentageToEvict()) / 100);
      return objects2Evict;
    }

    public String toString() {
      return "CacheStats[ type = " + type + ",\n\t usage = " + usage + ",\n\t countBefore = " + countBefore
             + ", toEvict = " + toEvict + ", evicted = " + evicted + ", countAfter = " + countAfter
             + ", objectsGCed = " + objectsGCed + ",\n\t state = " + state + "]";
    }
  }

}
