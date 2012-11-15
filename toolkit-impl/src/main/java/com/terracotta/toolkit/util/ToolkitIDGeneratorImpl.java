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
    map.putIfAbsent(name, new Long(1));
  }

  @Override
  public long getId() {
    return (Long) map.get(name);
  }

  @Override
  public void incrementId() {
    lock.lock();
    try {
      Long result = (Long) map.get(name) + 1;
      map.putNoReturn(name, result);
    } finally {
      lock.unlock();
    }
  }

}
