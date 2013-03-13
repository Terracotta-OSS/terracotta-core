/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;
import org.terracotta.toolkit.store.ToolkitStore;

import com.terracotta.toolkit.collections.ToolkitMapBlockingQueue;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;

/**
 * @author Eugene Shelestovich
 */
public class ToolkitMapBlockingQueueFactoryImpl implements ToolkitObjectFactory<ToolkitMapBlockingQueue> {

  public static final String CAPACITY_FIELD_NAME = "capacity";
  private static final String LOCK_POSTFIX = "_lock";
  private static final String STORE_POSTFIX = "_store";

  private final ToolkitInternal toolkit;

  public ToolkitMapBlockingQueueFactoryImpl(ToolkitInternal toolkit) {
    this.toolkit = toolkit;
  }

  @Override
  public ToolkitMapBlockingQueue getOrCreate(String name, Configuration config) {
    final ToolkitReadWriteLock lock = toolkit.getReadWriteLock(name + LOCK_POSTFIX);
    final Configuration storeConfig = new ToolkitStoreConfigBuilder()
        .consistency(ToolkitConfigFields.Consistency.STRONG)
        .concurrency(2)
        .localCacheEnabled(true)
        .maxCountLocalHeap(10000)
        .build();
    final ToolkitStore<String, Object> backedStore = toolkit.getStore(name + STORE_POSTFIX, storeConfig, null);
    return new ToolkitMapBlockingQueue(name, config.getInt(CAPACITY_FIELD_NAME), backedStore, lock);
  }

  @Override
  public ToolkitObjectType getManufacturedToolkitObjectType() {
    return ToolkitObjectType.BLOCKING_QUEUE;
  }
}
