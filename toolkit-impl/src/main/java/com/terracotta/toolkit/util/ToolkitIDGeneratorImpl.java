/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitStore;

public class ToolkitIDGeneratorImpl implements ToolkitIDGenerator {
  private final ToolkitLock  lock;
  private final String       name;
  private final ToolkitStore map;

  public ToolkitIDGeneratorImpl(String name, ToolkitStore map) {
    this.name = name;
    this.map = map;
    this.lock = map.createLockForKey(name).writeLock();
    // do a get first time to init the value
    getFromMap();
  }

  @Override
  public long getId() {
    return getFromMap();
  }

  private Long getFromMap() {
    Long rv = (Long) map.get(name);
    if (rv != null) {
      return rv;
    } else {
      lock.lock();
      try {
        // Need to manually do a put if absent here so that we avoid calling putIfAbsent unconditionally. The problem
        // is that putIfAbsent will trigger throttling (possibly throwing an exception) if the server is full. This
        // would
        // prevent toolkit from bootstrapping, thereby preventing the user from spinning up a new toolkit client in
        // order
        // to recover from the server full situation.
        rv = (Long) map.get(name);
        if (rv == null) {
          rv = 1L;
          map.put(name, rv);
        }
        return rv;
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public void incrementId() {
    lock.lock();
    try {
      Long result = getFromMap() + 1;
      map.putNoReturn(name, result);
    } finally {
      lock.unlock();
    }
  }

}
