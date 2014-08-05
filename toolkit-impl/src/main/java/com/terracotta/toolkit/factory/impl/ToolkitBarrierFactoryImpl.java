/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinLifecycleListener;
import com.terracotta.toolkit.concurrent.ToolkitBarrierImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.util.ToolkitIDGeneratorImpl;
import com.terracotta.toolkit.util.collections.WeakValueMap;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ToolkitBarrierFactoryImpl implements ToolkitObjectFactory<ToolkitBarrier> {
  public static final String                     PARTIES_CONFIG_NAME  = "PARTIES";
  private final ToolkitStore                     barriers;
  private final ToolkitIDGeneratorImpl           barrierIdGenerator;
  private final WeakValueMap<ToolkitBarrierImpl> localCache;
  private final Lock                             lock;
  private volatile Set<String>                   beforeRejoinBarriers = Collections.emptySet();
  private final PlatformService                  platformService;

  public ToolkitBarrierFactoryImpl(ToolkitStore clusteredMap, WeakValueMapManager manager,
                                   PlatformService platformService) {
    this.barriers = clusteredMap;
    this.platformService = platformService;
    this.barrierIdGenerator = new ToolkitIDGeneratorImpl(ToolkitTypeConstants.TOOLKIT_BARRIER_UID_NAME, barriers);
    this.localCache = manager.createWeakValueMap();
    this.lock = new ReentrantLock();

    platformService.addRejoinLifecycleListener(new RejoinLifecycleListener() {

      @Override
      public void onRejoinStart() {
        beforeRejoinBarriers = localCache.keySet();
        for (String name : beforeRejoinBarriers) {
          ToolkitBarrierImpl barrier = localCache.get(name);
          if (barrier != null) {
            barrier.rejoinStarted();
          }
        }
      }

      @Override
      public void onRejoinComplete() {
        for (String name : beforeRejoinBarriers) {
          ToolkitBarrierImpl barrier = localCache.get(name);
          if (barrier != null) {
            barrier.rejoinCompleted();
          }
        }
        beforeRejoinBarriers = Collections.emptySet();
      }
    });
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
        } else if (barrier.getParties() != parties) { throw new IllegalArgumentException(
                                                                                         "ClusteredBarrier already exists for name '"
                                                                                             + name
                                                                                             + "' with different number of parties - "
                                                                                             + "requested:" + parties
                                                                                             + " existing:"
                                                                                             + barrier.getParties()); }
      }
    } finally {
      lock.unlock();
    }
    return barrier;
  }

  private ToolkitBarrier createToolkitType(String name, int parties) {
    ToolkitBarrierImpl barrier = new ToolkitBarrierImpl(name, parties, barriers, barrierIdGenerator, platformService);
    localCache.put(name, barrier);
    return barrier;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BARRIER;
  }
}
