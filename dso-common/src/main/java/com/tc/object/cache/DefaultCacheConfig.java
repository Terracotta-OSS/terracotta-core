/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;


public class DefaultCacheConfig implements CacheConfig {

  @Override
  public int getLeastCount() {
    return 2;
  }

  @Override
  public int getPercentageToEvict() {
    return 10;
  }

  @Override
  public long getSleepInterval() {
    return 3000;
  }

  @Override
  public int getUsedCriticalThreshold() {
    return 90;
  }

  @Override
  public int getUsedThreshold() {
    return 70;
  }

  @Override
  public boolean isOnlyOldGenMonitored() {
    return true;
  }

  @Override
  public boolean isLoggingEnabled() {
    return false;
  }

  @Override
  public int getObjectCountCriticalThreshold() {
    return -1;
  }

}
