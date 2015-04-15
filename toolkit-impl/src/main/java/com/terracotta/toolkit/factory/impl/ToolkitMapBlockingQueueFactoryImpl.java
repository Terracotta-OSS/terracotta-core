/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.ToolkitMapBlockingQueue;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.util.collections.WeakValueMap;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eugene Shelestovich
 */
public class ToolkitMapBlockingQueueFactoryImpl implements ToolkitObjectFactory<ToolkitMapBlockingQueue> {

  public static final String CAPACITY_FIELD_NAME = "capacity";
  private static final String LOCK_POSTFIX = "_lock";
  private static final String STORE_POSTFIX = "_store";

  private final ToolkitInternal toolkit;
  private final WeakValueMap<ToolkitMapBlockingQueue> localCache;
  private final Lock localLock;
  private final PlatformService platformService;

  public ToolkitMapBlockingQueueFactoryImpl(final ToolkitInternal toolkit,
                                            final ToolkitFactoryInitializationContext context) {
    this.toolkit = toolkit;
    localCache = context.getWeakValueMapManager().createWeakValueMap();
    platformService = context.getPlatformService();
    localLock = new ReentrantLock();
  }

  @Override
  public ToolkitMapBlockingQueue getOrCreate(String name, Configuration config) {
    final int capacity = config.getInt(CAPACITY_FIELD_NAME);
    ToolkitMapBlockingQueue queue = null;
    localLock.lock();
    try {
      queue = localCache.get(name);
      if (queue == null) {
        queue = createQueue(name, capacity);
      } else {
        if (queue.isDestroyed()) {
          queue = createQueue(name, capacity);
        } else if (queue.getCapacity() != capacity) {
          throw new IllegalArgumentException("ToolkitBlockingQueue already exists for name '"
                                             + name + "' with different capacity requested: "
                                             + capacity + ", existing: " + queue.getCapacity());
        }
      }
    } finally {
      localLock.unlock();
    }
    return queue;
  }

  private ToolkitMapBlockingQueue createQueue(final String name, final int capacity) {
    final Configuration storeConfig = new ToolkitStoreConfigBuilder()
        .consistency(ToolkitConfigFields.Consistency.STRONG)
        .concurrency(2)
        .localCacheEnabled(true)
        .maxCountLocalHeap(10000)
        .build();

    final ToolkitReadWriteLock lock = ToolkitLockingApi.createUnnamedReadWriteLock(ToolkitObjectType.BLOCKING_QUEUE,
        name + LOCK_POSTFIX, platformService, ToolkitLockTypeInternal.WRITE);
    final ToolkitStore<String, Object> backedStore = toolkit.getStore(name + STORE_POSTFIX, storeConfig, null);
    final ToolkitMapBlockingQueue queue = new ToolkitMapBlockingQueue(name, capacity, backedStore, lock);
    localCache.put(name, queue);
    return queue;
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BLOCKING_QUEUE;
  }
}
