/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.properties.TCProperties;

public class CacheConfigImpl implements CacheConfig {

  private final int         leastCount;
  private final int         percentageToEvict;
  private final long        sleepInterval;
  private final int         criticalThreshold;
  private final int         threshold;
  private final boolean     monitorOldGenOnly;
  private final boolean     loggingEnabled;
  private final int         criticalObjectThreshold;

  private static final int  THRESHOLD                                = 70;
  private static final int  CRITICAL_THRESHOLD                       = 90;

  private static final int  TEMP_SWAP_OFFHEAP_2GB_THRESHOLD          = 50;
  private static final int  TEMP_SWAP_OFFHEAP_2GB_CRITICAL_THRESHOLD = 70;

  private static final long TWO_GB                                   = 2 * 1024 * 1024 * 1024L;

  public CacheConfigImpl(TCProperties cacheManagerProperties, boolean isPersistent, boolean isOffHeap) {
    this(cacheManagerProperties, cacheManagerProperties.getInt("threshold",
                                                               getDefaultThreshold(isPersistent, isOffHeap)),
         cacheManagerProperties.getInt("criticalThreshold", getDefaultCriticalThreshold(isPersistent, isOffHeap)));
  }

  public CacheConfigImpl(TCProperties cacheManagerProperties) {
    this(cacheManagerProperties, cacheManagerProperties.getInt("threshold"), cacheManagerProperties
        .getInt("criticalThreshold"));
  }

  public CacheConfigImpl(TCProperties cacheManagerProperties, int threshold, int criticalThreshold) {
    this.leastCount = cacheManagerProperties.getInt("leastCount");
    this.percentageToEvict = cacheManagerProperties.getInt("percentageToEvict");
    this.sleepInterval = cacheManagerProperties.getLong("sleepInterval");
    this.monitorOldGenOnly = cacheManagerProperties.getBoolean("monitorOldGenOnly");
    this.loggingEnabled = cacheManagerProperties.getBoolean("logging.enabled");
    this.criticalObjectThreshold = cacheManagerProperties.getInt("criticalObjectThreshold");
    this.threshold = threshold;
    this.criticalThreshold = criticalThreshold;
  }

  private static int getDefaultThreshold(boolean isPersistent, boolean isOffHeap) {
    if (!isPersistent && isOffHeap && getMaxMemory() <= TWO_GB) { return TEMP_SWAP_OFFHEAP_2GB_THRESHOLD; }
    return THRESHOLD;
  }

  private static long getMaxMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.maxMemory();
  }

  private static int getDefaultCriticalThreshold(boolean isPersistent, boolean isOffHeap) {
    if (!isPersistent && isOffHeap && getMaxMemory() <= TWO_GB) { return TEMP_SWAP_OFFHEAP_2GB_CRITICAL_THRESHOLD; }
    return CRITICAL_THRESHOLD;
  }

  public int getLeastCount() {
    return leastCount;
  }

  public int getPercentageToEvict() {
    return percentageToEvict;
  }

  public long getSleepInterval() {
    return sleepInterval;
  }

  public int getUsedCriticalThreshold() {
    return criticalThreshold;
  }

  public int getUsedThreshold() {
    return threshold;
  }

  public boolean isOnlyOldGenMonitored() {
    return monitorOldGenOnly;
  }

  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public int getObjectCountCriticalThreshold() {
    return criticalObjectThreshold;
  }

}
