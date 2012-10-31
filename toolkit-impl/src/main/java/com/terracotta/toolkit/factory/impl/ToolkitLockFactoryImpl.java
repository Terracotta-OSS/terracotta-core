/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitLockFactoryImpl implements ToolkitObjectFactory<ToolkitLockImpl> {
  public static final String                  INTERNAL_LOCK_TYPE = "INTERNAL_LOCK_TYPE";
  private final WeakValueMap<ToolkitLockImpl> localCache;
  private final PlatformService               platformService;
  private final Lock                          lock;

  public ToolkitLockFactoryImpl(WeakValueMapManager manager, PlatformService platformService) {
    this.localCache = manager.createWeakValueMap();
    this.platformService = platformService;
    this.lock = new ReentrantLock();
  }

  @Override
  public ToolkitLockImpl getOrCreate(String name, Configuration config) {
    ToolkitLockImpl toolkitLock = null;
    lock.lock();
    try {
      toolkitLock = localCache.get(name);
      if (toolkitLock == null) {
        ToolkitLockTypeInternal internalLockType = ToolkitLockTypeInternal
            .valueOf(config.getString(INTERNAL_LOCK_TYPE));
        toolkitLock = new ToolkitLockImpl(platformService, name, internalLockType);
        localCache.put(name, toolkitLock);
      }
    } finally {
      lock.unlock();
    }
    return toolkitLock;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.LOCK;
  }

}
