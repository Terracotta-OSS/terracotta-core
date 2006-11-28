/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.cache;


public class DefaultCacheConfig implements CacheConfig {

  public int getLeastCount() {
    return 2;
  }

  public int getPercentageToEvict() {
    return 10;
  }

  public long getSleepInterval() {
    return 3000;
  }

  public int getUsedCriticalThreshold() {
    return 90;
  }

  public int getUsedThreshold() {
    return 70;
  }

  public boolean isOnlyOldGenMonitored() {
    return true;
  }

  public boolean isLoggingEnabled() {
    return false;
  }

}
