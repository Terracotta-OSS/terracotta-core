/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.lang.TCThreadGroup;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCMemoryManagerImpl;
import com.tc.runtime.cache.CacheMemoryEventType;
import com.tc.runtime.cache.CacheMemoryEventsListener;
import com.tc.runtime.cache.CacheMemoryManagerEventGenerator;
import com.tc.statistics.AgentStatisticsManager;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.exceptions.AgentStatisticsManagerException;
import com.tc.util.Assert;
import com.tc.util.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class CacheManager implements CacheMemoryEventsListener {

  public static final String             CACHE_OBJECTS_EVICT_REQUEST = "cache objects evict request";
  public static final String             CACHE_OBJECTS_EVICTED       = "cache objects evicted";

  private static final TCLogger          logger                      = TCLogging.getLogger(CacheManager.class);

  private static final State             INIT                        = new State("INIT");
  private static final State             PROCESSING                  = new State("PROCESSING");
  private static final State             COMPLETE                    = new State("COMPLETE");

  private final Evictable                evictable;
  private final CacheConfig              config;

  private int                            calculatedCacheSize         = 0;
  private CacheStatistics                lastStat                    = null;
  private final StatisticsAgentSubSystem statisticsAgentSubSystem;
  private final TCMemoryManagerImpl      memoryManager;

  public CacheManager(Evictable evictable, CacheConfig config, TCThreadGroup threadGroup,
                      StatisticsAgentSubSystem statisticsAgentSubSystem, TCMemoryManagerImpl memoryManager) {
    this.evictable = evictable;
    this.config = config;
    this.memoryManager = memoryManager;

    if (config.getObjectCountCriticalThreshold() > 0) {
      logger
          .warn("Cache Object Count Critical threshold is set to "
                + config.getObjectCountCriticalThreshold()
                + ". It is not recommended that this value is set. Setting a wrong vlaue could totally destroy performance.");
    }
    this.statisticsAgentSubSystem = statisticsAgentSubSystem;
    Assert.assertNotNull(statisticsAgentSubSystem);
  }

  public void start() {
    new CacheMemoryManagerEventGenerator(config.getUsedThreshold(), config.getUsedCriticalThreshold(),
                                         config.getLeastCount(), memoryManager, this);
  }

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
    private final boolean              objectsGCed = false;
    private int                        toEvict;
    private long                       startTime;
    private State                      state       = INIT;

    public CacheStatistics(CacheMemoryEventType type, MemoryUsage usage) {
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
      final int usedPercentage = usage.getUsedPercentage();
      final long collectionCount = usage.getCollectionCount();
      if (config.isLoggingEnabled()) {
        logger.info("Asking to evict " + toEvict + " current size = " + currentCount + " calculated cache size = "
                    + calculatedCacheSize + " heap used = " + usedPercentage + " %  gc count = " + collectionCount);
      }
      if (statisticsAgentSubSystem.isActive()) {
        storeCacheEvictRequestStats(currentCount, toEvict, calculatedCacheSize, usedPercentage, collectionCount);
      }
      return this.toEvict;
    }

    private synchronized void storeCacheEvictRequestStats(int currentCount, int toEvictArg, int cacheSize,
                                                          int usedPercentage, long collectionCount) {
      Date moment = new Date();
      AgentStatisticsManager agentStatisticsManager = statisticsAgentSubSystem.getStatisticsManager();
      Collection sessions = agentStatisticsManager.getActiveSessionIDsForAction(CACHE_OBJECTS_EVICT_REQUEST);
      if (sessions != null && sessions.size() > 0) {
        StatisticData[] datas = getCacheObjectsEvictRequestData(currentCount, toEvictArg, cacheSize, usedPercentage,
                                                                collectionCount);
        storeStatisticsDatas(moment, sessions, datas);
      }
    }

    private synchronized StatisticData[] getCacheObjectsEvictRequestData(int currentCount, int toEvictArg,
                                                                         int cacheSize, int usedPercentage,
                                                                         long collectionCount) {
      List datas = new ArrayList();
      StatisticData statisticData = new StatisticData(CACHE_OBJECTS_EVICT_REQUEST, "asking to evict count",
                                                      Long.valueOf(toEvictArg));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICT_REQUEST, "current size", Long.valueOf(currentCount));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICT_REQUEST, "calculated cache size", Long.valueOf(cacheSize));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICT_REQUEST, "percentage heap used",
                                        Long.valueOf(usedPercentage));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICT_REQUEST, "gc count", Long.valueOf(collectionCount));
      datas.add(statisticData);

      return (StatisticData[]) datas.toArray(new StatisticData[0]);
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
      if (statisticsAgentSubSystem.isActive()) {
        storeCacheObjectsEvictedStats(evictedCount, currentCount, newObjectsCount, timeTaken);
      }
    }

    private synchronized void storeCacheObjectsEvictedStats(int evictedCount, int currentCount, int newObjectsCount,
                                                            long timeTaken) {
      Date moment = new Date();
      AgentStatisticsManager agentStatisticsManager = statisticsAgentSubSystem.getStatisticsManager();
      Collection sessions = agentStatisticsManager.getActiveSessionIDsForAction(CACHE_OBJECTS_EVICTED);
      if (sessions != null && sessions.size() > 0) {
        StatisticData[] datas = getCacheObjectsEvictedData(evictedCount, currentCount, newObjectsCount, timeTaken);
        storeStatisticsDatas(moment, sessions, datas);
      }
    }

    private synchronized void storeStatisticsDatas(Date moment, Collection sessions, StatisticData[] datas) {
      try {
        for (Iterator sessionsIterator = sessions.iterator(); sessionsIterator.hasNext();) {
          String session = (String) sessionsIterator.next();
          for (StatisticData data : datas) {
            statisticsAgentSubSystem.getStatisticsManager().injectStatisticData(session, data.moment(moment));
          }
        }
      } catch (AgentStatisticsManagerException e) {
        logger.error("Unexpected error while trying to store Cache Objects Evict Request statistics statistics.", e);
      }
    }

    private synchronized StatisticData[] getCacheObjectsEvictedData(int evictedCount, int currentCount,
                                                                    int newObjectsCount, long timeTaken) {
      List datas = new ArrayList();
      StatisticData statisticData = new StatisticData(CACHE_OBJECTS_EVICTED, "evicted count",
                                                      Long.valueOf(evictedCount));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICTED, "current count", Long.valueOf(currentCount));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICTED, "new objects count", Long.valueOf(newObjectsCount));
      datas.add(statisticData);

      statisticData = new StatisticData(CACHE_OBJECTS_EVICTED, "time taken", Long.valueOf(timeTaken));
      datas.add(statisticData);

      return (StatisticData[]) datas.toArray(new StatisticData[0]);
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
             + ", toEvict = " + toEvict + ", evicted = " + evicted + ", countAfter = " + countAfter
             + ", objectsGCed = " + objectsGCed + ",\n\t state = " + state + "]";
    }
  }

}
