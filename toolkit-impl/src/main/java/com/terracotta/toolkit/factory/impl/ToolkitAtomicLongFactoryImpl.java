/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLongImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitAtomicLongFactoryImpl implements ToolkitObjectFactory<ToolkitAtomicLong> {
  private final ToolkitStore           atomicLongs;
  private final ToolkitIDGeneratorImpl longIdGenerator;
  private final WeakValueMap<ToolkitAtomicLong> localCache;
  private final Lock                            lock;

  public ToolkitAtomicLongFactoryImpl(ToolkitStore atomicLongs, WeakValueMapManager manager) {
    this.atomicLongs = atomicLongs;
    this.longIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_LONG_UID_NAME, atomicLongs);
    this.localCache = manager.createWeakValueMap();
    this.lock = new ReentrantLock();
  }

  @Override
  public ToolkitAtomicLong getOrCreate(String name, Configuration config) {
    ToolkitAtomicLong atomicLong = null;
    lock.lock();
    try {
      atomicLong = localCache.get(name);
      if (atomicLong == null) {
        atomicLong = createToolkitType(name);
      } else {
        if (atomicLong.isDestroyed()) {
          atomicLong = createToolkitType(name);
        }
      }
    } finally {
      lock.unlock();
    }
    return atomicLong;
  }

  private ToolkitAtomicLong createToolkitType(String name) {
    ToolkitAtomicLong atomicLong = new ToolkitAtomicLongImpl(name, atomicLongs, longIdGenerator);
    localCache.put(name, atomicLong);
    return atomicLong;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.ATOMIC_LONG;
  }
}
