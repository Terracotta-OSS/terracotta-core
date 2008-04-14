/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cache;

import java.io.Serializable;

/**
 * Wrapper for a data value in the cache data store. This wrapper knows additional information about when the item was
 * created, when it was last used, when it expires, and whether it is still valid.
 */
public class CacheData implements Serializable {
  // Config
  private final CacheConfig config;

  // State (not modified)
  private final Object      value;
  private final Timestamp   timestamp;               // timestamp contains the time when the CacheData will be expired
  private final long        createTime;              // saved at creation time

  // State (modifiable) - guarded by synchronized methods on "this"
  private transient long    lastAccessedTimeInMillis;
  private boolean           invalidated;

  /**
   * @param value Data value, should never null (checked by caller)
   * @param config Cache configuration information
   */
  public CacheData(Object value, CacheConfig config) {
    this.config = config;
    this.value = value;
    this.createTime = System.currentTimeMillis();
    this.timestamp = new Timestamp(this.createTime, config.getMaxIdleTimeoutMillis(), config.getMaxTTLMillis());
    this.lastAccessedTimeInMillis = 0;
  }

  Timestamp getTimestamp() {
    return timestamp;
  }

  synchronized boolean isValid() {
    if (invalidated) { return false; }
    return hasNotExpired() && isStillAlive();
  }

  private boolean hasNotExpired() {
    if (config.getMaxIdleTimeoutMillis() <= 0) { return true; }
    return getIdleMillis() < config.getMaxIdleTimeoutMillis();
  }

  private boolean isStillAlive() {
    if (config.getMaxTTLMillis() <= 0) { return true; }
    return System.currentTimeMillis() <= getTimeToDieMillis();
  }

  private long getTimeToDieMillis() {
    return this.createTime + config.getMaxTTLMillis();
  }

  synchronized long getIdleMillis() {
    if (lastAccessedTimeInMillis == 0) return 0;
    return Math.max(System.currentTimeMillis() - lastAccessedTimeInMillis, 0);
  }

  synchronized void finish() {
    lastAccessedTimeInMillis = System.currentTimeMillis();
  }

  synchronized void accessed() {
    lastAccessedTimeInMillis = System.currentTimeMillis();
  }

  public synchronized Object getValue() {
    return value;
  }

  synchronized void invalidate() {
    this.invalidated = true;
  }

  synchronized boolean isInvalidated() {
    return this.invalidated;
  }

  public int hashCode() {
    return this.value.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof CacheData)) { return false; }
    CacheData cd = (CacheData) obj;
    return this.value.equals(cd.value);
  }

}
