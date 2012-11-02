/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import net.sf.ehcache.CacheManager;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
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
import org.terracotta.toolkit.store.ToolkitStore;
import org.terracotta.toolkit.store.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.net.GroupID;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.PlatformService;
import com.tc.object.bytecode.PlatformServiceImpl;
import com.terracotta.toolkit.cluster.TerracottaClusterInfo;
import com.terracotta.toolkit.collections.ToolkitBlockingQueueImpl;
import com.terracotta.toolkit.collections.ToolkitSetImpl;
import com.terracotta.toolkit.collections.ToolkitSortedSetImpl;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFactory;
import com.terracotta.toolkit.collections.servermap.api.ehcacheimpl.EhcacheSMLocalStoreFactory;
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
import com.terracotta.toolkit.factory.impl.ToolkitLockFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitMapFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitNotifierFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitReadWriteLockFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSetFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSortedMapFactoryImpl;
import com.terracotta.toolkit.factory.impl.ToolkitSortedSetFactoryImpl;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializationStrategyImpl;
import com.terracotta.toolkit.object.serialization.SerializerMap;
import com.terracotta.toolkit.object.serialization.SerializerMapImpl;
import com.terracotta.toolkit.roots.impl.RootsUtil;
import com.terracotta.toolkit.roots.impl.RootsUtil.RootObjectCreator;
import com.terracotta.toolkit.roots.impl.ToolkitTypeConstants;
import com.terracotta.toolkit.roots.impl.ToolkitTypeRootsStaticFactory;
import com.terracotta.toolkit.search.SearchFactory;
import com.terracotta.toolkit.search.UnsupportedSearchFactory;
import com.terracotta.toolkit.util.collections.WeakValueMapManager;

import java.util.UUID;

public class TerracottaToolkit implements ToolkitInternal {

  public static final String                                   TOOLKIT_SERIALIZER_REGISTRATION_NAME = "TOOLKIT_SERIALIZER";
  public static final String                                   PLATFORM_SERVICE_REGISTRATION_NAME   = "PLATFORM_SERVICE";
  private final ToolkitObjectFactory<ToolkitList>              clusteredListFactory;
  private final ToolkitObjectFactory<ToolkitCache>             clusteredCacheFactory;
  private final ToolkitObjectFactory<ToolkitMap>               clusteredMapFactory;
  private final ToolkitObjectFactory<ToolkitSortedMap>         clusteredSortedMapFactory;
  private final ToolkitObjectFactory<ToolkitCache>             clusteredStoreFactory;
  private final ToolkitObjectFactory<ToolkitAtomicLong>        clusteredAtomicLongFactory;
  private final ToolkitObjectFactory<ToolkitBarrier>           clusteredBarrierFactory;
  private final ToolkitObjectFactory<ToolkitNotifier>          clusteredNotifierFactory;
  private final ToolkitObjectFactory<ToolkitBlockingQueueImpl> clusteredBlockingQueueFactory;
  private final ToolkitObjectFactory<ToolkitSortedSetImpl>     clusteredSortedSetFactory;
  private final ToolkitObjectFactory<ToolkitSetImpl>           clusteredSetFactory;
  private final ToolkitObjectFactory<ToolkitLockImpl>          lockFactory;
  private final ToolkitObjectFactory<ToolkitReadWriteLockImpl> rwLockFactory;
  private final ServerMapLocalStoreFactory                     serverMapLocalStoreFactory;
  private final CacheManager                                   defaultToolkitCacheManager;
  private final TerracottaL1Instance                           tcClient;
  private final WeakValueMapManager                            weakValueMapManager                  = new WeakValueMapManager();
  private ToolkitProperties                                    toolkitProperties;

  protected final PlatformService                              platformService;
  private final ClusterInfo                                    clusterInfoInstance;

  public TerracottaToolkit(TerracottaL1Instance tcClient) {
    this.tcClient = tcClient;
    platformService = ManagerUtil.registerObjectByNameIfAbsent(PLATFORM_SERVICE_REGISTRATION_NAME,
                                                               new PlatformServiceImpl());
    clusterInfoInstance = new TerracottaClusterInfo(platformService);
    SerializationStrategy strategy = createSerializationStrategy();
    Object old = platformService.registerObjectByNameIfAbsent(TOOLKIT_SERIALIZER_REGISTRATION_NAME, strategy);
    if (old != null) {
      if (old instanceof SerializationStrategy) {
        strategy = (SerializationStrategy) old;
      } else {
        throw new AssertionError("Another object registered instead of serialization strategy - " + old);
      }
    }
    defaultToolkitCacheManager = createDefaultToolkitCacheManager();
    serverMapLocalStoreFactory = new EhcacheSMLocalStoreFactory(defaultToolkitCacheManager);
    ToolkitTypeRootsStaticFactory toolkitTypeRootsFactory = new ToolkitTypeRootsStaticFactory(weakValueMapManager);

    lockFactory = new ToolkitLockFactoryImpl(weakValueMapManager, platformService);
    rwLockFactory = new ToolkitReadWriteLockFactoryImpl(weakValueMapManager, platformService);
    clusteredNotifierFactory = new ToolkitNotifierFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    clusteredListFactory = new ToolkitListFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    // create set factory before map factory, as map uses set internally
    clusteredSetFactory = new ToolkitSetFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    clusteredCacheFactory = ToolkitCacheFactoryImpl.newToolkitCacheFactory(this, toolkitTypeRootsFactory,
                                                                           createSearchFactory(),
                                                                           serverMapLocalStoreFactory, platformService);
    clusteredMapFactory = new ToolkitMapFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    clusteredSortedMapFactory = new ToolkitSortedMapFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    clusteredStoreFactory = ToolkitCacheFactoryImpl.newToolkitStoreFactory(this, toolkitTypeRootsFactory,
                                                                           createSearchFactory(),
                                                                           serverMapLocalStoreFactory, platformService);
    clusteredBlockingQueueFactory = new ToolkitBlockingQueueFactoryImpl(this, toolkitTypeRootsFactory, platformService);
    ToolkitStore atomicLongs = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_ATOMIC_LONG_MAP_NAME,
                                                                 new ToolkitStoreConfigBuilder()
                                                                     .consistency(Consistency.STRONG).build());
    clusteredAtomicLongFactory = new ToolkitAtomicLongFactoryImpl(atomicLongs, weakValueMapManager);

    ToolkitStore barriers = clusteredStoreFactory.getOrCreate(ToolkitTypeConstants.TOOLKIT_BARRIER_MAP_NAME,
                                                              new ToolkitStoreConfigBuilder()
                                                                  .consistency(Consistency.STRONG).build());
    clusteredBarrierFactory = new ToolkitBarrierFactoryImpl(barriers, weakValueMapManager);

    clusteredSortedSetFactory = new ToolkitSortedSetFactoryImpl(this, toolkitTypeRootsFactory, platformService);
  }

  private CacheManager createDefaultToolkitCacheManager() {
    String cacheManagerUniqueName = "toolkitDefaultCacheManager-" + UUID.randomUUID().toString();
    return CacheManager.newInstance(new net.sf.ehcache.config.Configuration().name(cacheManagerUniqueName));
  }

  private SerializationStrategy createSerializationStrategy() {
    return new SerializationStrategyImpl(platformService, getOrCreateSerializerRootMap(platformService));
  }

  private static SerializerMap getOrCreateSerializerRootMap(PlatformService platformService) {
    return RootsUtil.lookupOrCreateRootInGroup(platformService, new GroupID(0),
                                               ToolkitTypeConstants.SERIALIZER_MAP_ROOT_NAME,
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
    return clusteredBlockingQueueFactory.getOrCreate(name, new UnclusteredConfiguration()
        .setInt(ToolkitBlockingQueueFactoryImpl.CAPACITY_FIELD_NAME, capacity));
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
    return clusteredCacheFactory.getOrCreate(name, configuration);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, new ToolkitCacheConfigBuilder().build(), null);
  }

  @Override
  public boolean isCapabilityEnabled(String capability) {
    return false;
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
    platformService.waitForAllCurrentTransactionsToComplete();
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
}
