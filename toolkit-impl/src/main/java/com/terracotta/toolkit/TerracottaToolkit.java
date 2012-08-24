/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import net.sf.ehcache.CacheManager;

import org.terracotta.toolkit.ToolkitCapability;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.TerracottaL1Instance;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.net.GroupID;
import com.tc.object.bytecode.ManagerUtil;
import com.terracotta.toolkit.cluster.TerracottaClusterInfo;
import com.terracotta.toolkit.collections.ToolkitBlockingQueueImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.collections.servermap.api.ehcacheimpl.EhcacheSMLocalStoreStaticFactory;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockImpl;
import com.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLockImpl;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.events.OperatorEventUtil;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.factory.impl.ToolkitAtomicLongFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitBarrierFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitBlockingQueueFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitCacheFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitListFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitMapFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitNotifierFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSetFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSortedSetFactoryImpl;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializationStrategyImpl;
import com.terracotta.toolkit.object.serialization.SerializerMap;
import com.terracotta.toolkit.object.serialization.SerializerMapImpl;
import com.terracotta.toolkit.roots.impl.RootsUtil;
import com.terracotta.toolkit.roots.impl.RootsUtil.RootObjectCreator;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootsStaticFactory;
import com.terracotta.toolkit.search.SearchBuilderFactory;
import com.terracotta.toolkit.search.UnsupportedSearchBuilderFactory;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.UUID;

public class TerracottaToolkit implements ToolkitInternal {

  public static final String                                   TOOLKIT_SERIALIZER_REGISTRATION_NAME = "TOOLKIT_SERIALIZER";
  private final ToolkitObjectFactory<ToolkitList>              clusteredListFactory;
  private final ToolkitObjectFactory<ToolkitCache>             clusteredCacheFactory;
  private final ToolkitObjectFactory<ToolkitMap>               clusteredMapFactory;
  private final ToolkitObjectFactory<ToolkitCache>             clusteredStoreFactory;
  private final ToolkitObjectFactory<ToolkitAtomicLong>        clusteredAtomicLongFactory;
  private final ToolkitObjectFactory<ToolkitBarrier>           clusteredBarrierFactory;
  private final ToolkitObjectFactory<ToolkitNotifier>          clusteredNotifierFactory;
  private final ToolkitObjectFactory<ToolkitBlockingQueueImpl> clusteredBlockingQueueFactory;
  private final ToolkitObjectFactory<ToolkitSortedSet>         clusteredSortedSetFactory;
  private final ToolkitObjectFactory<ToolkitSet>               clusteredSetFactory;
  private final ServerMapLocalStoreFactory                     serverMapLocalStoreFactory;
  private final CacheManager                                   defaultToolkitCacheManager;
  private final TerracottaL1Instance                           tcClient;
  private final WeakValueMapManager                            weakValueMapManager                  = new WeakValueMapManager();
  private ToolkitProperties                                    toolkitProperties;

  public TerracottaToolkit(TerracottaL1Instance tcClient) {
    this.tcClient = tcClient;
    SerializationStrategy strategy = createSerializationStrategy();
    Object old = ManagerUtil.registerObjectByNameIfAbsent(TOOLKIT_SERIALIZER_REGISTRATION_NAME, strategy);
    if (old != null) {
      if (old instanceof SerializationStrategy) {
        strategy = (SerializationStrategy) old;
      } else {
        throw new AssertionError("Another object registered instead of serialization strategy - " + old);
      }
    }

    defaultToolkitCacheManager = createDefaultToolkitCacheManager();
    serverMapLocalStoreFactory = new EhcacheSMLocalStoreStaticFactory(defaultToolkitCacheManager);

    ToolkitTypeRootsStaticFactory toolkitTypeRootsFactory = new ToolkitTypeRootsStaticFactory(weakValueMapManager);

    clusteredNotifierFactory = new ToolkitNotifierFactoryImpl(this, toolkitTypeRootsFactory);
    clusteredListFactory = new ToolkitListFactoryImpl(this, toolkitTypeRootsFactory);
    // create set factory before map factory, as map uses set internally
    clusteredSetFactory = new ToolkitSetFactoryImpl(this, toolkitTypeRootsFactory);
    clusteredCacheFactory = ToolkitCacheFactoryImpl.newToolkitCacheFactory(this, toolkitTypeRootsFactory,
                                                                           createSearchBuilderFactory(),
                                                                           serverMapLocalStoreFactory);
    clusteredMapFactory = new ToolkitMapFactoryImpl(this, toolkitTypeRootsFactory);
    clusteredStoreFactory = ToolkitCacheFactoryImpl.newToolkitStoreFactory(this, toolkitTypeRootsFactory,
                                                                           createSearchBuilderFactory(),
                                                                           serverMapLocalStoreFactory);
    clusteredBlockingQueueFactory = new ToolkitBlockingQueueFactoryImpl(this, toolkitTypeRootsFactory);
    ToolkitStore atomicLongs = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_ATOMIC_LONG_MAP_NAME,
                                                                 new ToolkitStoreConfigBuilder()
                                                                     .consistency(Consistency.STRONG).build());
    clusteredAtomicLongFactory = new ToolkitAtomicLongFactoryImpl(atomicLongs);

    ToolkitStore barriers = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_BARRIER_MAP_NAME,
                                                              new ToolkitStoreConfigBuilder()
                                                                  .consistency(Consistency.STRONG).build());
    clusteredBarrierFactory = new ToolkitBarrierFactoryImpl(barriers);

    clusteredSortedSetFactory = new ToolkitSortedSetFactoryImpl(this, toolkitTypeRootsFactory);
  }

  private CacheManager createDefaultToolkitCacheManager() {
    String cacheManagerUniqueName = "toolkitDefaultCacheManager-" + UUID.randomUUID().toString();
    return CacheManager.newInstance(new net.sf.ehcache.config.Configuration().name(cacheManagerUniqueName));
  }

  private SerializationStrategy createSerializationStrategy() {
    return new SerializationStrategyImpl(getOrCreateSerializerRootMap());
  }

  private static SerializerMap getOrCreateSerializerRootMap() {
    return RootsUtil.lookupOrCreateRootInGroup(new GroupID(0), ToolkitTypeConstants.SERIALIZER_MAP_ROOT_NAME,
                                               new RootObjectCreator<SerializerMapImpl>() {
                                                 @Override
                                                 public SerializerMapImpl create() {
                                                   return new SerializerMapImpl();
                                                 }
                                               });
  }

  @Override
  public <E> ToolkitList<E> getList(String name, Class<E> klazz) {
    return clusteredListFactory.getOrCreate(name, null);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Configuration configuration, Class<V> klazz) {
    return clusteredStoreFactory.getOrCreate(name, configuration);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Class<V> klazz) {
    return getStore(name, new ToolkitStoreConfigBuilder().build(), null);
  }

  @Override
  public <K, V> ToolkitMap<K, V> getMap(String name, Class<K> keyKlazz, Class<V> valueKlazz) {
    return clusteredMapFactory.getOrCreate(name, null);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return TerracottaClusterInfoHolder.INSTANCE;
  }

  @Override
  public ToolkitLock getLock(String name) {
    return getLock(name, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public ToolkitLock getLock(String name, ToolkitLockTypeInternal internalLockType) {
    return new ToolkitLockImpl(name, internalLockType);
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock(String name) {
    return new ToolkitReadWriteLockImpl(name);
  }

  @Override
  public <M> ToolkitNotifier<M> getNotifier(String name, Class<M> klazz) {
    return clusteredNotifierFactory.getOrCreate(name, null);
  }

  @Override
  public ToolkitAtomicLong getAtomicLong(String name) {
    return clusteredAtomicLongFactory.getOrCreate(name, null);
  }

  @Override
  public ToolkitBarrier getBarrier(String name, int parties) {
    if (parties < 1) { throw new IllegalArgumentException("Number of parties should be at least 1 - " + parties); }
    return clusteredBarrierFactory.getOrCreate(name, new UnclusteredConfiguration()
        .setInt(ToolkitBarrierFactoryImpl.PARTIES_CONFIG_NAME, parties));
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, int capacity, Class<E> klazz) {
    if (capacity < 1) { throw new IllegalArgumentException("Capacity should be at least 1 - " + capacity); }
    return clusteredBlockingQueueFactory.getOrCreate(name, new UnclusteredConfiguration()
        .setInt(ToolkitBlockingQueueFactoryImpl.CAPACITY_FIELD_NAME, capacity));
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, Class<E> klazz) {
    return getBlockingQueue(name, Integer.MAX_VALUE, null);
  }

  @Override
  public void fireOperatorEvent(OperatorEventLevel level, String applicationName, String eventMessage) {
    OperatorEventUtil.fireOperatorEvent(level, applicationName, eventMessage);
  }

  @Override
  public <E extends Comparable<? super E>> ToolkitSortedSet<E> getSortedSet(String name, Class<E> klazz) {
    return clusteredSortedSetFactory.getOrCreate(name, null);
  }

  @Override
  public <E> ToolkitSet<E> getSet(String name, Class<E> klazz) {
    return clusteredSetFactory.getOrCreate(name, null);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Configuration configuration, Class<V> klazz) {
    return clusteredCacheFactory.getOrCreate(name, configuration);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, new ToolkitCacheConfigBuilder().build(), null);
  }

  @Override
  public boolean isCapabilityEnabled(ToolkitCapability capability) {
    switch (capability) {
      case SEARCH:
        return false;
      case OFFHEAP:
        return false;
    }
    // don't define default, so as to catch missing case at compile time
    throw new AssertionError("Unhandled capability - " + capability);
  }

  private static class TerracottaClusterInfoHolder {
    private static ClusterInfo INSTANCE = new TerracottaClusterInfo();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    ManagerUtil.getManager().registerBeforeShutdownHook(hook);
  }

  @Override
  public ToolkitLogger getLogger(String name) {
    return new TerracottaLogger(name);
  }

  @Override
  public void waitUntilAllTransactionsComplete() {
    ManagerUtil.waitForAllCurrentTransactionsToComplete();
  }

  protected SearchBuilderFactory createSearchBuilderFactory() {
    return UnsupportedSearchBuilderFactory.INSTANCE;
  }

  @Override
  public synchronized void shutdown() {
    weakValueMapManager.cancel();
    try {
      tcClient.shutdown();
    } finally {
      defaultToolkitCacheManager.shutdown();
    }
  }

  @Override
  public String getClientUUID() {
    return ManagerUtil.getUUID();
  }

  @Override
  public void isCapabilityEnabled(String capability) {
    ManagerUtil.verifyCapability(capability);
  }

  @Override
  public synchronized ToolkitProperties getProperties() {
    if (toolkitProperties == null) {
      toolkitProperties = new TerracottaProperties();
    }
    return toolkitProperties;
  }
}
