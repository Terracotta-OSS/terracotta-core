/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import net.sf.ehcache.CacheManager;

import org.terracotta.toolkit.ToolkitFeature;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.ToolkitFeatureTypeInternal;
import org.terracotta.toolkit.atomic.ToolkitTransactionController;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
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
import org.terracotta.toolkit.store.ToolkitConfigFields.Consistency;
import org.terracotta.toolkit.store.ToolkitStore;

import com.google.common.base.Preconditions;
import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.atomic.ToolkitTransactionFeatureImpl;
import com.terracotta.toolkit.cluster.TerracottaClusterInfo;
import com.terracotta.toolkit.collections.DestroyableToolkitList;
import com.terracotta.toolkit.collections.DestroyableToolkitMap;
import com.terracotta.toolkit.collections.DestroyableToolkitSortedMap;
import com.terracotta.toolkit.collections.ToolkitBlockingQueueImpl;
import com.terracotta.toolkit.collections.ToolkitMapBlockingQueue;
import com.terracotta.toolkit.collections.ToolkitSetImpl;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.collections.map.ToolkitCacheImpl;
import com.terracotta.toolkit.collections.servermap.api.ehcacheimpl.EhcacheSMLocalStoreFactory;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockImpl;
import com.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLockImpl;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.events.DestroyableToolkitNotifier;
import com.terracotta.toolkit.events.OperatorEventUtil;
import com.terracotta.toolkit.factory.ToolkitFactoryInitializationContext;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.factory.impl.ToolkitAtomicLongFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitBarrierFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitBlockingQueueFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitCacheFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitFactoryInitializationContextBuilder;
import com.terracotta.toolkit.factory.impl.ToolkitListFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitLockFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitMapBlockingQueueFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitMapFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitNotifierFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitReadWriteLockFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSetFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSortedMapFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSortedSetFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitStoreFactoryImpl;
import com.terracotta.toolkit.feature.NoopLicenseFeature;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializationStrategyImpl;
import com.terracotta.toolkit.object.serialization.ThreadContextAwareClassLoader;
import com.terracotta.toolkit.object.serialization.UserSuppliedClassLoader;
import com.terracotta.toolkit.rejoin.PlatformServiceProvider;
import com.terracotta.toolkit.rejoin.RejoinAwareSerializerMap;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootsStaticFactory;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.search.UnsupportedSearchFactory;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

public class TerracottaToolkit implements ToolkitInternal {

  public static final String                                      TOOLKIT_SERIALIZER_REGISTRATION_NAME = "TOOLKIT_SERIALIZER";
  private static final int                                        QUEUE_THRESHOLD = 2000000;
  private final ToolkitObjectFactory<DestroyableToolkitList>      clusteredListFactory;
  private final ToolkitObjectFactory<ToolkitCacheImpl>            clusteredCacheFactory;
  private final ToolkitObjectFactory<DestroyableToolkitMap>       clusteredMapFactory;
  private final ToolkitObjectFactory<DestroyableToolkitSortedMap> clusteredSortedMapFactory;
  private final ToolkitObjectFactory<ToolkitCacheImpl>            clusteredStoreFactory;
  private final ToolkitObjectFactory<ToolkitAtomicLong>           clusteredAtomicLongFactory;
  private final ToolkitObjectFactory<ToolkitBarrier>              clusteredBarrierFactory;
  private final ToolkitObjectFactory<DestroyableToolkitNotifier>  clusteredNotifierFactory;
  private final ToolkitObjectFactory<ToolkitBlockingQueueImpl>    clusteredBlockingQueueFactory;
  private final ToolkitObjectFactory<ToolkitMapBlockingQueue>     clusteredMapBlockingQueueFactory;
  private final ToolkitObjectFactory<ToolkitSortedSetImpl>        clusteredSortedSetFactory;
  private final ToolkitObjectFactory<ToolkitSetImpl>              clusteredSetFactory;
  private final ToolkitObjectFactory<ToolkitLockImpl>             lockFactory;
  private final ToolkitObjectFactory<ToolkitReadWriteLockImpl>    rwLockFactory;
  private final CacheManager                                      defaultToolkitCacheManager;
  private final TerracottaL1Instance                              tcClient;
  private final WeakValueMapManager                               weakValueMapManager                  = new WeakValueMapManager();
  private ToolkitProperties                                       toolkitProperties;
  protected final PlatformService                                 platformService;
  private final ClusterInfo                                       clusterInfoInstance;
  protected final boolean                                         isNonStop;
  private final ToolkitTransactionController                      transactionController;

  public TerracottaToolkit(TerracottaL1Instance tcClient, ToolkitCacheManagerProvider toolkitCacheManagerProvider,
                           boolean isNonStop, ClassLoader loader) {

    this.tcClient = tcClient;
    this.isNonStop = isNonStop;
    this.platformService = PlatformServiceProvider.getPlatformService();
    clusterInfoInstance = new TerracottaClusterInfo(platformService);
    SerializationStrategy strategy = createSerializationStrategy(loader);
    Object old = platformService.registerObjectByNameIfAbsent(TOOLKIT_SERIALIZER_REGISTRATION_NAME, strategy);
    if (old != null) {
      if (old instanceof SerializationStrategy) {
        strategy = (SerializationStrategy) old;
      } else {
        throw new AssertionError("Another object registered instead of serialization strategy - " + old);
      }
    }
    this.defaultToolkitCacheManager = toolkitCacheManagerProvider.getDefaultCacheManager();

    ToolkitFactoryInitializationContextBuilder builder = new ToolkitFactoryInitializationContextBuilder();
    final ToolkitFactoryInitializationContext context = builder.weakValueMapManager(weakValueMapManager)
        .platformService(platformService)
        .toolkitTypeRootsFactory(new ToolkitTypeRootsStaticFactory(weakValueMapManager))
        .serverMapLocalStoreFactory(new EhcacheSMLocalStoreFactory(defaultToolkitCacheManager))
        .searchFactory(createSearchFactory()).build();

    lockFactory = new ToolkitLockFactoryImpl(context);
    rwLockFactory = new ToolkitReadWriteLockFactoryImpl(context);
    clusteredNotifierFactory = new ToolkitNotifierFactoryImpl(this, context);
    clusteredListFactory = new ToolkitListFactoryImpl(this, context);
    // create set factory before map factory, as map uses set internally
    clusteredSetFactory = new ToolkitSetFactoryImpl(this, context);
    clusteredMapFactory = new ToolkitMapFactoryImpl(this, context);
    clusteredCacheFactory = ToolkitCacheFactoryImpl.newToolkitCacheFactory(this, context);
    clusteredSortedMapFactory = new ToolkitSortedMapFactoryImpl(this, context);
    clusteredStoreFactory = ToolkitStoreFactoryImpl.newToolkitStoreFactory(this, context);
    clusteredBlockingQueueFactory = new ToolkitBlockingQueueFactoryImpl(this, context);
    clusteredMapBlockingQueueFactory = new ToolkitMapBlockingQueueFactoryImpl(this, context);

    ToolkitStore atomicLongs = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_ATOMIC_LONG_MAP_NAME,
                                                                 new ToolkitStoreConfigBuilder()
                                                                     .consistency(Consistency.STRONG).build());
    clusteredAtomicLongFactory = new ToolkitAtomicLongFactoryImpl(atomicLongs, weakValueMapManager, platformService);

    ToolkitStore barriers = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_BARRIER_MAP_NAME,
                                                              new ToolkitStoreConfigBuilder()
                                                                  .consistency(Consistency.STRONG).build());
    clusteredBarrierFactory = new ToolkitBarrierFactoryImpl(barriers, weakValueMapManager, platformService);

    clusteredSortedSetFactory = new ToolkitSortedSetFactoryImpl(this, context);
    transactionController = new ToolkitTransactionFeatureImpl(platformService);
  }

  private SerializationStrategy createSerializationStrategy(ClassLoader loader) {
    if (loader == null) {
      loader = new ThreadContextAwareClassLoader(getClass().getClassLoader());
    } else {
      loader = new UserSuppliedClassLoader(loader, getClass().getClassLoader());
    }

    RejoinAwareSerializerMap map = getOrCreateSerializerRootMap();
    platformService.addRejoinLifecycleListener(map);
    return new SerializationStrategyImpl(this.platformService, map, loader);
  }

  private RejoinAwareSerializerMap getOrCreateSerializerRootMap() {
    return new RejoinAwareSerializerMap(this.platformService);
  }

  @Override
  public <E> ToolkitList<E> getList(String name, Class<E> klazz) {
    return clusteredListFactory.getOrCreate(name, null);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Configuration configuration, Class<V> klazz) {
    if (configuration == null) {
      configuration = new ToolkitStoreConfigBuilder().build();
    }
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
  public <K extends Comparable<? super K>, V> ToolkitSortedMap<K, V> getSortedMap(String name, Class<K> keyKlazz,
                                                                                  Class<V> valueKlazz) {
    return clusteredSortedMapFactory.getOrCreate(name, null);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return clusterInfoInstance;
  }

  @Override
  public ToolkitLock getLock(String name) {
    return getOrCreateLock(name, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public ToolkitLock getLock(String name, ToolkitLockTypeInternal internalLockType) {
    return getOrCreateLock(name, internalLockType);
  }

  private ToolkitLock getOrCreateLock(String name, ToolkitLockTypeInternal internalLockType) {
    return lockFactory.getOrCreate(name, new UnclusteredConfiguration()
        .setString(ToolkitLockFactoryImpl.INTERNAL_LOCK_TYPE, internalLockType.name()));
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock(String name) {
    return rwLockFactory.getOrCreate(name, null);
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
    if (capacity > QUEUE_THRESHOLD) {
      // scales better for big queues, but has significantly lower throughput
      return clusteredMapBlockingQueueFactory.getOrCreate(name, new UnclusteredConfiguration()
          .setInt(ToolkitMapBlockingQueueFactoryImpl.CAPACITY_FIELD_NAME, capacity));
    } else {
      return clusteredBlockingQueueFactory.getOrCreate(name, new UnclusteredConfiguration()
          .setInt(ToolkitBlockingQueueFactoryImpl.CAPACITY_FIELD_NAME, capacity));
    }
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, Class<E> klazz) {
    return getBlockingQueue(name, Integer.MAX_VALUE, null);
  }

  @Override
  public void fireOperatorEvent(OperatorEventLevel level, String applicationName, String eventMessage) {
    OperatorEventUtil.fireOperatorEvent(platformService, level, applicationName, eventMessage);
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
    if (configuration == null) {
      configuration = new ToolkitCacheConfigBuilder().build();
    }
    return clusteredCacheFactory.getOrCreate(name, configuration);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, null, klazz);
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    platformService.registerBeforeShutdownHook(hook);
  }

  @Override
  public ToolkitLogger getLogger(String name) {
    return new TerracottaLogger(name, platformService);
  }

  @Override
  public void waitUntilAllTransactionsComplete() {
    try {
      platformService.waitForAllCurrentTransactionsToComplete();
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
  }

  protected SearchFactory createSearchFactory() {
    return UnsupportedSearchFactory.INSTANCE;
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
    return platformService.getUUID();
  }

  @Override
  public synchronized ToolkitProperties getProperties() {
    if (toolkitProperties == null) {
      toolkitProperties = new TerracottaProperties(platformService);
    }
    return toolkitProperties;
  }

  AbortableOperationManager getAbortableOperationManager() {
    return platformService.getAbortableOperationManager();
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureType<T> type) {
    Preconditions.checkNotNull(type);
    return ToolkitInstanceProxy.newFeatureNotSupportedProxy(type.getFeatureClass());
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureTypeInternal<T> type) {
    Preconditions.checkNotNull(type);
    if (type == ToolkitFeatureTypeInternal.TRANSACTION) { return (T) transactionController; }
    if (type == ToolkitFeatureTypeInternal.LICENSE) { return (T) NoopLicenseFeature.SINGLETON; }
    return ToolkitInstanceProxy.newFeatureNotSupportedProxy(type.getFeatureClass());
  }
}
