/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedItem {
  private final ConcurrentHashMap parentCache;
  private final LockID            lockID;
  private final Object            key;
  private volatile Object         value;
  private CachedItem              next;

  public CachedItem(final ConcurrentHashMap cache, final LockID lockID, final Object key, final Object value) {
    this.parentCache = cache;
    this.lockID = lockID;
    this.key = key;
    this.value = value;
  }

  public Map getParentCache() {
    return this.parentCache;
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public Object getKey() {
    return this.key;
  }

  public Object getValue() {
    return this.value;
  }

  public void clear() {
    this.value = null;
  }

  public void dispose() {
    this.parentCache.remove(this.key);
  }

  public CachedItem getNext() {
    return this.next;
  }

  public void setNext(final CachedItem next) {
    this.next = next;
  }
}