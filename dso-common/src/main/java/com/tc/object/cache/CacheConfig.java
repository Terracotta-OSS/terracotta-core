/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
