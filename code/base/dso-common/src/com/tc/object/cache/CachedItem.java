/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.cache.ExpirableEntry;
import com.tc.object.locks.LockID;

public class CachedItem {

  public interface DisposeListener {
    public void disposed(CachedItem ci);
  }

  private final DisposeListener listener;
  private final LockID          lockID;
  private final Object          key;
  private volatile Object       value;
  private volatile boolean      accessed = true;
  private volatile boolean      expired  = false;
  private CachedItem            next;

  public CachedItem(final DisposeListener listener, final LockID lockID, final Object key, final Object value) {
    this.listener = listener;
    this.lockID = lockID;
    this.key = key;
    this.value = value;
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public Object getKey() {
    return this.key;
  }

  public ExpirableEntry getExpirableEntry() {
    if (this.value instanceof ExpirableEntry) { return (ExpirableEntry) this.value; }
    return null;
  }

  public Object getValue() {
    this.accessed = true;
    return this.value;
  }

  public void clear() {
    this.accessed = true;
    this.value = null;
  }

  public void dispose() {
    this.listener.disposed(this);
  }

  public CachedItem getNext() {
    return this.next;
  }

  public void setNext(final CachedItem next) {
    this.next = next;
  }

  public boolean getAndClearAccessed() {
    final boolean isAccessed = this.accessed;
    this.accessed = false;
    return isAccessed;
  }

  public void setAccessed() {
    this.accessed = true;
  }

  public boolean isExpired() {
    return this.expired;
  }

  public void markExpired() {
    this.expired = true;
  }
}