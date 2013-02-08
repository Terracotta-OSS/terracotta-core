/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.cache.CacheMemoryEventType;
import com.tc.runtime.cache.CacheMemoryEventsListener;
import com.tc.runtime.cache.CacheMemoryManagerEventGenerator;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheManager implements CacheMemoryEventsListener {

  private static final TCLogger          logger                      = TCLogging.getLogger(CacheManager.class);

  private static final State             INIT                        = new State("INIT");
  private static final State             PROCESSING                  = new State("PROCESSING");
  private static final State             COMPLETE                    = new State("COMPLETE");

  private final Evictable                evictable;
  private final CacheConfig              config;

  private int                            calculatedCacheSize         = 0;
  private CacheStatistics                lastStat                    = null;
  private final TCMemoryManagerImpl      memoryManager;

  public CacheManager(Evictable evictable, CacheConfig config, TCMemoryManagerImpl memoryManager) {
    this.evictable = evictable;
    this.config = config;
    this.memoryManager = memoryManager;

    if (config.getObjectCountCriticalThreshold() > 0) {
      logger
          .warn("Cache Object Count Critical threshold is set to "
                + config.getObjectCountCriticalThreshold()
                + ". It is not recommended that this value is set. Setting a wrong vlaue could totally destroy performance.");
    }
  }

  public void start() {
    new CacheMemoryManagerEventGenerator(config.getUsedThreshold(), config.getUsedCriticalThreshold(),
                                         config.getLeastCount(), memoryManager, this);
  }

  @Override
  public void memoryUsed(CacheMemoryEventType type, MemoryUsage usage) {
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
    private final CacheMemoryEventType type;
    private final MemoryUsage          usage;

    private int                        countBefore;
    private int                        countAfter;
    private int                        evicted;
    private int                        toEvict;
    private long                       startTime;
    private State                      state = INIT;

    private long                       lastLoggingTime;              // timestamp to store the time of last log
    private final long                 logIntervalMillis = TimeUnit.SECONDS.toMillis(5L); // 5 seconds

    public CacheStatistics(CacheMemoryEventType type, MemoryUsage usage) {
      this.type = type;
      this.usage = usage;
      this.lastLoggingTime = System.currentTimeMillis();
    }

    public void validate() {
      if (state == PROCESSING) {
        // This might be ignored by the memory manager thread. TODO:: exit VM !!!
        throw new AssertionError(this + " : Object Evicted is not called. This indicates a bug in the software !");
      }
    }

    @Override
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
      final int usedPercentage = usage.getUsedPercentage();
      final long collectionCount = usage.getCollectionCount();
      if (config.isLoggingEnabled()) {
        // Mechanism to control the amount of logging.
        if (toEvict > 0 || (System.currentTimeMillis() - lastLoggingTime > logIntervalMillis)) {
          lastLoggingTime = System.currentTimeMillis();
          logger.info("Asking to evict " + toEvict + " current size = " + currentCount + " calculated cache size = "
                      + calculatedCacheSize + " heap used = " + usedPercentage + " %  gc count = " + collectionCount);
        }
      }
      return this.toEvict;
    }

    //
    // We recalibrate the calculatedCacheSize based on the current known details if one of the following is true.
    // 0) Usage goes below threshold or
    // 1) This is the first threshold crossing alarm or
    // 2) A GC has taken place since the last time (we either base in on the collection count which is accurate in 1.5
    // or in 1.4 we check to see if the usedMemory has gone down which is an indication of gc (not foolprove though)
    //
    private void adjustCachedObjectCount(int currentCount) {
      if (type == CacheMemoryEventType.BELOW_THRESHOLD || lastStat == null
          || lastStat.usage.getCollectionCount() < usage.getCollectionCount()
          || (usage.getCollectionCount() < 0 && lastStat.usage.getUsedMemory() > usage.getUsedMemory())) {
        double used = usage.getUsedPercentage();
        double threshold = config.getUsedThreshold();
        Assert.assertTrue((type == CacheMemoryEventType.BELOW_THRESHOLD && threshold >= used) || threshold <= used);
        if (config.getObjectCountCriticalThreshold() > 0) {
          // set calculated cache size to critical object threshold
          calculatedCacheSize = config.getObjectCountCriticalThreshold();
        } else if (used > 0) {
          calculatedCacheSize = (int) (currentCount * (threshold / used));
        }
      }
    }

    @Override
    public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC, boolean printNewObjects) {
      this.evicted = evictedCount;
      this.countAfter = currentCount;
      state = COMPLETE;
      // TODO:: add reference queue
      final int newObjectsCount = getNewObjectsCount();
      final long timeTaken = System.currentTimeMillis() - startTime;
      if (config.isLoggingEnabled()) {
        if (!printNewObjects) {
          logger.info("Evicted " + evictedCount + " current Size = " + currentCount + " time taken = " + timeTaken
                      + " ms");
        } else {
          logger.info("Evicted " + evictedCount + " current Size = " + currentCount + " new objects created = "
                      + newObjectsCount + " time taken = " + timeTaken + " ms");
        }
      }
    }

    private int getNewObjectsCount() {
      return countAfter - (countBefore - evicted);
    }

    private int computeObjects2Evict(int currentCount) {
      if (type == CacheMemoryEventType.BELOW_THRESHOLD || calculatedCacheSize > currentCount) { return 0; }
      int overshoot = currentCount - calculatedCacheSize;
      if (overshoot <= 0) { return 0; }
      int objects2Evict = overshoot + ((calculatedCacheSize * config.getPercentageToEvict()) / 100);
      // CDV-592 : With a higher percentage to evict, the calculated value could cross the currentCount
      return (objects2Evict > currentCount ? currentCount : objects2Evict);
    }

    @Override
    public String toString() {
      return "CacheStats[ type = " + type + ",\n\t usage = " + usage + ",\n\t countBefore = " + countBefore
             + ", toEvict = " + toEvict + ", evicted = " + evicted + ", countAfter = " + countAfter + ", \n\t state = "
             + state + "]";
    }
  }

}
