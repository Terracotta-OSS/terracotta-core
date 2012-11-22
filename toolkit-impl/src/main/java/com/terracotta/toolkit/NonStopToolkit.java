/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.ToolkitObjectType;
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
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.nonstop.NonStopManager;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.object.ToolkitObject;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.collections.map.ValuesResolver;
import com.terracotta.toolkit.nonstop.NonStopConfigRegistryImpl;
import com.terracotta.toolkit.nonstop.NonStopDelegateProvider;
import com.terracotta.toolkit.nonstop.NonStopInvocationHandler;
import com.terracotta.toolkit.nonstop.NonStopManagerImpl;
import com.terracotta.toolkit.nonstop.NonStopToolkitCacheDelegateProvider;
import com.terracotta.toolkit.nonstop.NonstopTimeoutBehaviorResolver;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopToolkit implements ToolkitInternal {
  private final ToolkitInternal                                                        toolkitDelegate;
  private final NonStopManager                                                         nonStopManager;
  private final NonStopConfigRegistryImpl                                              nonStopConfigManager          = new NonStopConfigRegistryImpl();
  private final NonstopTimeoutBehaviorResolver                                         nonstopTimeoutBehaviorFactory = new NonstopTimeoutBehaviorResolver();

  private final ConcurrentMap<ToolkitObjectType, ConcurrentMap<String, ToolkitObject>> toolkitObjects                = new ConcurrentHashMap<ToolkitObjectType, ConcurrentMap<String, ToolkitObject>>();

  public NonStopToolkit(ToolkitInternal toolkitDelegate) {
    this.toolkitDelegate = toolkitDelegate;
    if (!(toolkitDelegate instanceof TerracottaToolkit)) { throw new RuntimeException(
                                                                                      "Wrong Toolkit Initialized, toolkit should be instance of "
                                                                                          + TerracottaToolkit.class
                                                                                          + " but is "
                                                                                          + toolkitDelegate.getClass()); }

    AbortableOperationManager abortableOperationManager = ((TerracottaToolkit) toolkitDelegate)
        .getAbortableOperationManager();
    this.nonStopManager = new NonStopManagerImpl(abortableOperationManager);

    for (ToolkitObjectType objectType : ToolkitObjectType.values()) {
      toolkitObjects.put(objectType, new ConcurrentHashMap<String, ToolkitObject>());
    }
  }

  private ToolkitInternal getInitializedToolkit() {
    return toolkitDelegate;
  }

  @Override
  public <E> ToolkitList<E> getList(String name, Class<E> klazz) {
    return getInitializedToolkit().getList(name, klazz);
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Configuration configuration, Class<V> klazz) {
    // TODO: refactor in a factory ?
    ToolkitStore<String, V> store = (ToolkitStore<String, V>) toolkitObjects.get(ToolkitObjectType.STORE).get(name);
    if (store != null) { return store; }
    NonStopDelegateProvider<ToolkitCacheInternal<String, V>> nonStopDelegateProvider = new NonStopToolkitCacheDelegateProvider(
                                                                                                                               nonStopConfigManager,
                                                                                                                               nonstopTimeoutBehaviorFactory,
                                                                                                                               toolkitDelegate,
                                                                                                                               name,
                                                                                                                               klazz,
                                                                                                                               configuration);
    store = (ToolkitCache<String, V>) Proxy
        .newProxyInstance(this.getClass().getClassLoader(), new Class[] { ToolkitCacheInternal.class,
                              ValuesResolver.class },
                          new NonStopInvocationHandler<ToolkitCacheInternal<String, V>>(nonStopManager,
                                                                                        nonStopDelegateProvider));
    ToolkitCache<String, V> oldStore = (ToolkitCache<String, V>) toolkitObjects.get(ToolkitObjectType.STORE)
        .putIfAbsent(name, store);
    return oldStore == null ? store : oldStore;
  }

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Class<V> klazz) {
    return getStore(name, null, klazz);
  }

  @Override
  public <K, V> ToolkitMap<K, V> getMap(String name, Class<K> keyKlazz, Class<V> valueKlazz) {
    return getInitializedToolkit().getMap(name, keyKlazz, valueKlazz);
  }

  @Override
  public <K extends Comparable<? super K>, V> ToolkitSortedMap<K, V> getSortedMap(String name, Class<K> keyKlazz,
                                                                                  Class<V> valueKlazz) {
    return getInitializedToolkit().getSortedMap(name, keyKlazz, valueKlazz);
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, int capacity, Class<E> klazz) {
    return getInitializedToolkit().getBlockingQueue(name, capacity, klazz);
  }

  @Override
  public <E> ToolkitBlockingQueue<E> getBlockingQueue(String name, Class<E> klazz) {
    return getInitializedToolkit().getBlockingQueue(name, klazz);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return getInitializedToolkit().getClusterInfo();
  }

  @Override
  public ToolkitLock getLock(String name) {
    return getInitializedToolkit().getLock(name);
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock(String name) {
    return getInitializedToolkit().getReadWriteLock(name);
  }

  @Override
  public <E> ToolkitNotifier<E> getNotifier(String name, Class<E> klazz) {
    return getInitializedToolkit().getNotifier(name, klazz);
  }

  @Override
  public ToolkitAtomicLong getAtomicLong(String name) {
    return getInitializedToolkit().getAtomicLong(name);
  }

  @Override
  public ToolkitBarrier getBarrier(String name, int parties) {
    return getInitializedToolkit().getBarrier(name, parties);
  }

  @Override
  public void fireOperatorEvent(OperatorEventLevel level, String applicationName, String eventMessage) {
    getInitializedToolkit().fireOperatorEvent(level, applicationName, eventMessage);
  }

  @Override
  public <E extends Comparable<? super E>> ToolkitSortedSet<E> getSortedSet(String name, Class<E> klazz) {
    return getInitializedToolkit().getSortedSet(name, klazz);
  }

  @Override
  public <E> ToolkitSet<E> getSet(String name, Class<E> klazz) {
    return getInitializedToolkit().getSet(name, klazz);
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Configuration configuration, Class<V> klazz) {
    // TODO: refactor in a factory ?
    ToolkitCache<String, V> cache = (ToolkitCache<String, V>) toolkitObjects.get(ToolkitObjectType.CACHE).get(name);
    if (cache != null) { return cache; }
    NonStopDelegateProvider<ToolkitCacheInternal<String, V>> nonStopDelegateProvider = new NonStopToolkitCacheDelegateProvider(
                                                                                                                               nonStopConfigManager,
                                                                                                                               nonstopTimeoutBehaviorFactory,
                                                                                                                               toolkitDelegate,
                                                                                                                               name,
                                                                                                                               klazz,
                                                                                                                               configuration);
    cache = (ToolkitCache<String, V>) Proxy
        .newProxyInstance(this.getClass().getClassLoader(), new Class[] { ToolkitCacheInternal.class,
                              ValuesResolver.class },
                          new NonStopInvocationHandler<ToolkitCacheInternal<String, V>>(nonStopManager,
                                                                                        nonStopDelegateProvider));
    ToolkitCache<String, V> oldCache = (ToolkitCache<String, V>) toolkitObjects.get(ToolkitObjectType.CACHE)
        .putIfAbsent(name, cache);
    return oldCache == null ? cache : oldCache;
  }

  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, null, klazz);
  }

  @Override
  public boolean isCapabilityEnabled(String capability) {
    return getInitializedToolkit().isCapabilityEnabled(capability);
  }

  @Override
  public void shutdown() {
    getInitializedToolkit().shutdown();
  }

  @Override
  public NonStopConfigurationRegistry getNonStopToolkitRegistry() {
    return nonStopConfigManager;
  }

  @Override
  public ToolkitLock getLock(String name, ToolkitLockTypeInternal lockType) {
    return getInitializedToolkit().getLock(name, lockType);
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    getInitializedToolkit().registerBeforeShutdownHook(hook);
  }

  @Override
  public void waitUntilAllTransactionsComplete() {
    getInitializedToolkit().waitUntilAllTransactionsComplete();
  }

  @Override
  public ToolkitLogger getLogger(String name) {
    return getInitializedToolkit().getLogger(name);
  }

  @Override
  public String getClientUUID() {
    return getInitializedToolkit().getClientUUID();
  }

  @Override
  public ToolkitProperties getProperties() {
    return getInitializedToolkit().getProperties();
  }

  @Override
  public NonStopManager getNonStopManager() {
    return nonStopManager;
  }
}
