/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.ToolkitFeature;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.ToolkitFeatureTypeInternal;
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
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.monitoring.OperatorEventLevel;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.abortable.AbortableOperationManager;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookup;
import com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookupAsync;
import com.terracotta.toolkit.nonstop.NonStopClusterListener;
import com.terracotta.toolkit.nonstop.NonStopConfigRegistryImpl;
import com.terracotta.toolkit.nonstop.NonStopConfigurationLookup;
import com.terracotta.toolkit.nonstop.NonStopContext;
import com.terracotta.toolkit.nonstop.NonStopContextImpl;
import com.terracotta.toolkit.nonstop.NonStopLockImpl;
import com.terracotta.toolkit.nonstop.NonStopManagerImpl;
import com.terracotta.toolkit.nonstop.NonstopTimeoutBehaviorResolver;
import com.terracotta.toolkit.nonstop.ToolkitLockLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.concurrent.FutureTask;

public class NonStopToolkitImpl implements ToolkitInternal {
  protected final NonStopManagerImpl           nonStopManager;
  protected final NonStopConfigRegistryImpl    nonStopConfigManager          = new NonStopConfigRegistryImpl();
  private final NonstopTimeoutBehaviorResolver nonstopTimeoutBehaviorFactory = new NonstopTimeoutBehaviorResolver();

  private final AbortableOperationManager      abortableOperationManager;
  protected final NonStopClusterListener       nonStopClusterListener;
  private final NonStopFeature                        nonStopFeature;
  private final AsyncToolkitInitializer        asyncToolkitInitializer;
  private final NonStopContext                 context;
  private final NonStopClusterInfo             nonStopClusterInfo;
  private final PlatformService                platformService;
  private final NonStopInitializationService   nonStopInitiailzationService;

  public NonStopToolkitImpl(FutureTask<ToolkitInternal> toolkitDelegateFutureTask, PlatformService platformService) {
    this.platformService = platformService;
    this.abortableOperationManager = platformService.getAbortableOperationManager();
    this.nonStopManager = new NonStopManagerImpl(abortableOperationManager);
    this.nonStopConfigManager.registerForType(NonStopConfigRegistryImpl.DEFAULT_CONFIG,
                                              NonStopConfigRegistryImpl.SUPPORTED_TOOLKIT_TYPES
                                                  .toArray(new ToolkitObjectType[0]));
    this.nonStopFeature = new NonStopFeatureImpl(this, abortableOperationManager);
    this.asyncToolkitInitializer = new AsyncToolkitInitializer(toolkitDelegateFutureTask, abortableOperationManager);
    this.nonStopClusterInfo = new NonStopClusterInfo(asyncToolkitInitializer);
    this.nonStopClusterListener = new NonStopClusterListener(abortableOperationManager, nonStopClusterInfo);
    this.context = new NonStopContextImpl(nonStopManager, nonStopConfigManager, abortableOperationManager,
                                          nonstopTimeoutBehaviorFactory, asyncToolkitInitializer,
                                          nonStopClusterListener);

    this.nonStopInitiailzationService = new NonStopInitializationService(context);
  }

  private ToolkitInternal getInitializedToolkit() {
    return asyncToolkitInitializer.getToolkit();
  }

  @Override
  public <E> ToolkitList<E> getList(final String name, final Class<E> klazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.LIST, context, ToolkitListInternal.class,
                                                new AbstractToolkitObjectLookup<ToolkitList<E>>(
                                                    abortableOperationManager) {

                                                  @Override
                                                  public ToolkitList<E> lookupObject() {
                                                    return getInitializedToolkit().getList(name, klazz);
                                                  }
                                                });
  }


  @Override
  public <K, V> ToolkitMap<K, V> getMap(final String name, final Class<K> keyKlazz, final Class<V> valueKlazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.MAP, context, ToolkitMap.class,
                                                new AbstractToolkitObjectLookup<ToolkitMap>(abortableOperationManager) {
                                                  @Override
                                                  public ToolkitMap<K, V> lookupObject() {
                                                    return getInitializedToolkit().getMap(name, keyKlazz, valueKlazz);
                                                  }
                                                });
  }

  @Override
  public <K extends Comparable<? super K>, V> ToolkitSortedMap<K, V> getSortedMap(final String name,
                                                                                  final Class<K> keyKlazz,
                                                                                  final Class<V> valueKlazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.SORTED_MAP, context, ToolkitSortedMap.class,
                                                new AbstractToolkitObjectLookup<ToolkitSortedMap>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitSortedMap<K, V> lookupObject() {
                                                    return getInitializedToolkit().getSortedMap(name, keyKlazz,
                                                                                                valueKlazz);
                                                  }
                                                });
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
    return nonStopClusterInfo;
  }

  @Override
  public ToolkitLock getLock(String name) {
    return getLock(name, ToolkitLockTypeInternal.WRITE);
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock(final String name) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.READ_WRITE_LOCK, context,
                                                ToolkitReadWriteLock.class,
                                                new AbstractToolkitObjectLookup<ToolkitReadWriteLock>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitReadWriteLock lookupObject() {
                                                    return getInitializedToolkit().getReadWriteLock(name);
                                                  }
                                                });
  }

  @Override
  public <E> ToolkitNotifier<E> getNotifier(final String name, final Class<E> klazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.NOTIFIER, context, ToolkitNotifier.class,
                                                new AbstractToolkitObjectLookup<ToolkitNotifier<E>>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitNotifier<E> lookupObject() {
                                                    return getInitializedToolkit().getNotifier(name, klazz);
                                                  }
                                                });
  }

  @Override
  public ToolkitAtomicLong getAtomicLong(final String name) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.ATOMIC_LONG, context, ToolkitAtomicLong.class,
                                                new AbstractToolkitObjectLookup<ToolkitAtomicLong>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitAtomicLong lookupObject() {
                                                    return getInitializedToolkit().getAtomicLong(name);
                                                  }
                                                });
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
  public <E extends Comparable<? super E>> ToolkitSortedSet<E> getSortedSet(final String name, final Class<E> klazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.SORTED_SET, context, ToolkitSortedSet.class,
                                                new AbstractToolkitObjectLookup<ToolkitSortedSet<E>>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitSortedSet<E> lookupObject() {
                                                    return getInitializedToolkit().getSortedSet(name, klazz);
                                                  }
                                                });
  }

  @Override
  public <E> ToolkitSet<E> getSet(final String name, final Class<E> klazz) {
    return ToolkitInstanceProxy.newNonStopProxy(name, ToolkitObjectType.SET, context, ToolkitSet.class,
                                                new AbstractToolkitObjectLookup<ToolkitSet<E>>(
                                                    abortableOperationManager) {
                                                  @Override
                                                  public ToolkitSet<E> lookupObject() {
                                                    return getInitializedToolkit().getSet(name, klazz);
                                                  }
                                                });
  }


  @Override
  public <V> ToolkitCache<String, V> getCache(final String name, final Configuration configuration, final Class<V> klazz) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.CACHE);

    final AbstractToolkitObjectLookupAsync<ToolkitCache> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitCache>(
        name, abortableOperationManager) {
      @Override
      public ToolkitCache<String, V> lookupObject() {
        return getInitializedToolkit().getCache(name, configuration, klazz);
      }
    };
    
    nonStopInitiailzationService.initialize(toolkitObjectLookup,
                                                nonStopConfigurationLookup.getNonStopConfiguration());

    return ToolkitInstanceProxy.newNonStopProxy(nonStopConfigurationLookup, context, ToolkitCacheInternal.class,
                                                toolkitObjectLookup);
  }


  @Override
  public <V> ToolkitCache<String, V> getCache(String name, Class<V> klazz) {
    return getCache(name, null, klazz);
  }


  @Override
  public <V> ToolkitStore<String, V> getStore(final String name, final Configuration configuration, final Class<V> klazz) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.STORE);

    final AbstractToolkitObjectLookupAsync<ToolkitStore> toolkitObjectLookup = new AbstractToolkitObjectLookupAsync<ToolkitStore>(
        name, abortableOperationManager) {
      @Override
      public ToolkitStore<String, V> lookupObject() {
        return getInitializedToolkit().getStore(name, configuration, klazz);
      }
    };
    
    nonStopInitiailzationService.initialize(toolkitObjectLookup,
                                                nonStopConfigurationLookup.getNonStopConfiguration());

    return ToolkitInstanceProxy.newNonStopProxy(nonStopConfigurationLookup, context, ToolkitStore.class,
                                                toolkitObjectLookup);
  }
  

  @Override
  public <V> ToolkitStore<String, V> getStore(String name, Class<V> klazz) {
    return getStore(name, null, klazz);
  }

  @Override
  public void shutdown() {
    nonStopManager.shutdown();
    getInitializedToolkit().shutdown();
    nonStopInitiailzationService.shutdown();
  }

  public NonStopConfigurationRegistry getNonStopConfigurationToolkitRegistry() {
    return nonStopConfigManager;
  }

  public void start(NonStopConfiguration configuration) {
    nonStopConfigManager.registerForThread(configuration);

    if (configuration.isEnabled()) {
      nonStopManager.begin(configuration.getTimeoutMillis());
    }
  }

  public void stop() {
    NonStopConfiguration configuration = nonStopConfigManager.deregisterForThread();

    if (configuration != null && configuration.isEnabled()) {
      nonStopManager.finish();
    }
  }

  @Override
  public ToolkitLock getLock(final String name, final ToolkitLockTypeInternal lockType) {
    NonStopConfigurationLookup nonStopConfigurationLookup = getNonStopConfigurationLookup(name, ToolkitObjectType.LOCK);
    
    ToolkitLockLookup toolkitObjectLookup = new ToolkitLockLookup(asyncToolkitInitializer, name, lockType);
    return new NonStopLockImpl(context, nonStopConfigurationLookup, toolkitObjectLookup);
  }

  @Override
  public void registerBeforeShutdownHook(Runnable hook) {
    getInitializedToolkit().registerBeforeShutdownHook(hook);
  }

  @Override
  public void waitUntilAllTransactionsComplete() {
    try {
      getInitializedToolkit().waitUntilAllTransactionsComplete();
    } catch (ToolkitAbortableOperationException e) {
      throw new NonStopException(e);
    }
  }

  @Override
  public ToolkitLogger getLogger(String name) {
    return getInitializedToolkit().getLogger(name);
  }

  @Override
  public String getClientUUID() {
    return platformService.getUUID();
  }

  @Override
  public ToolkitProperties getProperties() {
    return getInitializedToolkit().getProperties();
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureType<T> type) {
    if (type == ToolkitFeatureType.NONSTOP) { return (T) nonStopFeature; }
    return getInitializedToolkit().getFeature(type);
  }

  @Override
  public <T extends ToolkitFeature> T getFeature(ToolkitFeatureTypeInternal<T> type) {
    return getInitializedToolkit().getFeature(type);
  }

  private NonStopConfigurationLookup getNonStopConfigurationLookup(final String name, final ToolkitObjectType objectType) {
    return new NonStopConfigurationLookup(context, objectType, name);
  }
}
