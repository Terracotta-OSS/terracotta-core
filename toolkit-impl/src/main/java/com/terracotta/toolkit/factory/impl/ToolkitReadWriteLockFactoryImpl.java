/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.config.Configuration;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLockImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitReadWriteLockFactoryImpl implements ToolkitObjectFactory<ToolkitReadWriteLockImpl> {
  private final WeakValueMap<ToolkitReadWriteLockImpl> localCache;
  private final PlatformService                    platformService;
  private final Lock                                   lock;

  public ToolkitReadWriteLockFactoryImpl(WeakValueMapManager manager, PlatformService platformService) {
    this.localCache = manager.createWeakValueMap();
    this.platformService = platformService;
    this.lock = new ReentrantLock();
  }

  @Override
  public ToolkitReadWriteLockImpl getOrCreate(String name, Configuration config) {
    ToolkitReadWriteLockImpl rwLock = null;
    lock.lock();
    try {
      rwLock = localCache.get(name);
      if (rwLock == null) {
        rwLock = new ToolkitReadWriteLockImpl(platformService, name);
        localCache.put(name, rwLock);
      }
    } finally {
      lock.unlock();
    }
    return rwLock;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.READ_WRITE_LOCK;
  }

}
