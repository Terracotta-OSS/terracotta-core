/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
}
