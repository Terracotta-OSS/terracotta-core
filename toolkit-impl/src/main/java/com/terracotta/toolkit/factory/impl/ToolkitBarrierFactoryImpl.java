/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.concurrent.ToolkitBarrierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.ToolkitObjectType;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitBarrierFactoryImpl implements ToolkitObjectFactory<ToolkitBarrier> {
  public static final String           PARTIES_CONFIG_NAME = "PARTIES";
  private final ToolkitStore           barriers;
  private final ToolkitIDGeneratorImpl barrierIdGenerator;
  private final WeakValueMap<ToolkitBarrier> localCache;
  private final Lock                         lock;

  public ToolkitBarrierFactoryImpl(ToolkitStore clusteredMap, WeakValueMapManager manager) {
    this.barriers = clusteredMap;
    this.barrierIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_BARRIER_UID_NAME, barriers);
    this.localCache = manager.createWeakValueMap();
    this.lock = new ReentrantLock();
  }

  @Override
  public ToolkitBarrier getOrCreate(String name, Configuration config) {
    int parties = config.getInt(PARTIES_CONFIG_NAME);
    ToolkitBarrier barrier = null;
    lock.lock();
    try {
      barrier = localCache.get(name);
      if (barrier == null) {
        barrier = createToolkitType(name, parties);
      } else {
        if (barrier.isDestroyed()) {
          barrier = createToolkitType(name, parties);
        }
      }
    } finally {
      lock.unlock();
    }
    return barrier;
  }

  private ToolkitBarrier createToolkitType(String name, int parties) {
    ToolkitBarrier barrier = new ToolkitBarrierImpl(name, parties, barriers, barrierIdGenerator);
    localCache.put(name, barrier);
    return barrier;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BARRIER;
  }
}
