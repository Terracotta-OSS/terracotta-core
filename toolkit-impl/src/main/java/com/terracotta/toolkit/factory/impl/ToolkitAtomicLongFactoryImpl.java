/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLongImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitAtomicLongFactoryImpl implements ToolkitObjectFactory<ToolkitAtomicLong> {
  private final ToolkitStore                        atomicLongs;
  private final ToolkitIDGeneratorImpl              longIdGenerator;
  private final WeakValueMap<ToolkitAtomicLongImpl> localCache;
  private final Lock                                lock;
  private volatile Set<String>                      beforeRejoinSet = Collections.EMPTY_SET;
  private final PlatformService                     platformService;

  public ToolkitAtomicLongFactoryImpl(ToolkitStore atomicLongs, WeakValueMapManager manager,
                                      PlatformService platformService) {
    this.atomicLongs = atomicLongs;
    this.platformService = platformService;
    this.longIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_LONG_UID_NAME, atomicLongs);
    this.localCache = manager.createWeakValueMap();
    this.lock = new ReentrantLock();
    platformService.addRejoinLifecycleListener(new RejoinLifecycleListener() {

      @Override
      public void onRejoinStart() {
        beforeRejoinSet = localCache.keySet();
        for (String name : beforeRejoinSet) {
          ToolkitAtomicLongImpl atomicLong = localCache.get(name);
          if (atomicLong != null) {
            atomicLong.rejoinStarted();
          }
        }
      }

      @Override
      public void onRejoinComplete() {
        for (String name : beforeRejoinSet) {
          ToolkitAtomicLongImpl atomicLong = localCache.get(name);
          if (atomicLong != null) {
            atomicLong.rejoinCompleted();
          }
        }
        beforeRejoinSet = Collections.EMPTY_SET;
      }
    });
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
    ToolkitAtomicLongImpl atomicLong = new ToolkitAtomicLongImpl(name, atomicLongs, longIdGenerator, platformService);
    localCache.put(name, atomicLong);
    return atomicLong;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.ATOMIC_LONG;
  }
}
