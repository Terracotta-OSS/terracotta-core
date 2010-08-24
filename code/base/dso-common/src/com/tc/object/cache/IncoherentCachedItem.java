/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.locks.LockID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.concurrent.TimeUnit;

public class IncoherentCachedItem extends CachedItem {

  private static final long SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = TCPropertiesImpl
                                                                                      .getProperties()
                                                                                      .getLong(
                                                                                               TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_ITEMS_RECYCLE_TIME);

  private final long        lastCoherentTime;

  public IncoherentCachedItem(CachedItem item) {
    this(item.getListener(), item.getLockID(), item.getKey(), item.getValue());
  }

  public IncoherentCachedItem(DisposeListener listener, LockID lockID, Object key, Object value) {
    super(listener, lockID, key, value);
    this.lastCoherentTime = System.nanoTime();
  }

  public boolean isIncoherentTooLong() {
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }

}
