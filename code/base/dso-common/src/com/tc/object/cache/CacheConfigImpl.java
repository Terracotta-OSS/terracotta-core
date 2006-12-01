/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.cache;

import com.tc.properties.TCProperties;

public class CacheConfigImpl implements CacheConfig {

  private final int     leastCount;
  private final int     percentageToEvict;
  private final long    sleepInterval;
  private final int     criticalThreshold;
  private final int     threshold;
  private final boolean monitorOldGenOnly;
  private final boolean loggingEnabled;

  public CacheConfigImpl(TCProperties cacheManagerProperties) {
    leastCount = cacheManagerProperties.getInt("leastCount");
    percentageToEvict = cacheManagerProperties.getInt("percentageToEvict");
    sleepInterval = cacheManagerProperties.getLong("sleepInterval");
    criticalThreshold = cacheManagerProperties.getInt("criticalThreshold");
    threshold = cacheManagerProperties.getInt("threshold");
    monitorOldGenOnly = cacheManagerProperties.getBoolean("monitorOldGenOnly");
    loggingEnabled = cacheManagerProperties.getBoolean("logging.enabled");
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

}
