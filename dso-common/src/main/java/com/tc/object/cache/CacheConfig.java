/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

public interface CacheConfig {

  int getUsedThreshold();

  int getLeastCount();

  int getUsedCriticalThreshold();

  long getSleepInterval();

  int getPercentageToEvict();
  
  boolean isOnlyOldGenMonitored();

  boolean isLoggingEnabled();
  
  int getObjectCountCriticalThreshold();
}
