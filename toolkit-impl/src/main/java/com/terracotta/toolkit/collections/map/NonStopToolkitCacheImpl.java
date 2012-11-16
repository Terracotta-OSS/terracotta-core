/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SearchExecutor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.nonstop.NonStopConfigRegistryImpl;
import com.terracotta.toolkit.nonstop.NonStopManager;
import com.terracotta.toolkit.nonstop.NonstopTimeoutBehaviorResolver;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class NonStopToolkitCacheImpl<K, V> implements ValuesResolver<K, V>, ToolkitCacheInternal<K, V> {
  private final NonStopManager                                                    nonStopManager;
  private final NonStopConfigRegistryImpl                                         nonStopConfigManager;

  private final Configuration                                                     actualConfiguration;
  private final String                                                            name;
  private final Class<V>                                                          klazz;

  private final ToolkitInternal                                                   toolkit;
  private final AtomicReference<ToolkitCacheInternal<K, V>>                       delegate                 = new AtomicReference<ToolkitCacheInternal<K, V>>();

  private final ConcurrentMap<NonStopTimeoutBehavior, ToolkitCacheInternal<K, V>> timeoutBehaviorResolvers = new ConcurrentHashMap<NonStopTimeoutBehavior, ToolkitCacheInternal<K, V>>();
  private final NonstopTimeoutBehaviorResolver                                    behaviorResolver;

  public NonStopToolkitCacheImpl(String name, Class<V> klazz, Configuration actualConfiguration,
                                 NonStopManager nonStopManager, ToolkitInternal toolkitFutureTask,
                                 NonStopConfigRegistryImpl nonStopConfigManager,
                                 NonstopTimeoutBehaviorResolver behaviorResolverFactory) {
    this.name = name;
    this.klazz = klazz;
    this.actualConfiguration = actualConfiguration;
    this.nonStopManager = nonStopManager;
    this.toolkit = toolkitFutureTask;
    this.nonStopConfigManager = nonStopConfigManager;
    this.behaviorResolver = behaviorResolverFactory;
  }

  private ToolkitCacheInternal<K, V> resolveTimeoutBehavior() {
    NonStopTimeoutBehavior nonStopBehavior = nonStopConfigManager.getConfigForInstance(name, getObjectType())
        .getNonStopTimeoutBehavior();
    ToolkitCacheInternal<K, V> resolver = timeoutBehaviorResolvers.get(nonStopBehavior);
    if (resolver == null) {
      // TODO: should getDelegate() be also done asynchronously OR with a timeout?
      resolver = behaviorResolver.create(getObjectType(), nonStopBehavior, delegate);
      ToolkitCacheInternal<K, V> oldResolver = timeoutBehaviorResolvers.putIfAbsent(nonStopBehavior, resolver);
      resolver = oldResolver != null ? oldResolver : resolver;
    }

    return resolver;
  }

  private long getTimeout(String method) {
    return nonStopConfigManager.getConfigForInstanceMethod(method, name, getObjectType()).getTimeout();
  }

  protected ToolkitObjectType getObjectType() {
    return ToolkitObjectType.CACHE;
  }

  private ToolkitCacheInternal<K, V> getDelegate() {
    if (delegate.get() == null) {
      if (actualConfiguration == null) {
        delegate.set((ToolkitCacheInternal<K, V>) toolkit.getCache(name, klazz));
      } else {
        delegate.set((ToolkitCacheInternal<K, V>) toolkit.getCache(name, actualConfiguration, klazz));
      }
    }

    return delegate.get();
  }

  private String getMethod() {
    StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
    StackTraceElement e = stacktrace[1];// coz 0th will be getStackTrace so 1st
    return e.getMethodName();
  }

  @Override
  public String getName() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getName();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getName();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isDestroyed() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().isDestroyed();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().isDestroyed();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void destroy() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().destroy();
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().destroy();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V getQuiet(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getQuiet(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getQuiet(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getAllQuiet(keys);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getAllQuiet(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().addListener(listener);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().addListener(listener);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().removeListener(listener);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().removeListener(listener);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unpinAll() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().unpinAll();
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().unpinAll();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isPinned(K key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().isPinned(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().isPinned(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().setPinned(key, pinned);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().setPinned(key, pinned);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeNoReturn(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().removeNoReturn(key);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().removeNoReturn(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putNoReturn(K key, V value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().putNoReturn(key, value);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().putNoReturn(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getAll(keys);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getAll(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Configuration getConfiguration() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getConfiguration();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getConfiguration();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().setConfigField(name, value);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().setConfigField(name, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().containsValue(value);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().containsValue(value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().createLockForKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().createLockForKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().setAttributeExtractor(attrExtractor);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().setAttributeExtractor(attrExtractor);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().putIfAbsent(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().putIfAbsent(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean remove(Object key, Object value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().remove(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().remove(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V replace(K key, V value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().replace(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().replace(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().replace(key, oldValue, newValue);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().replace(key, oldValue, newValue);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void clear() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().clear();
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().clear();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().containsKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().containsKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().entrySet();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().entrySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V get(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().get(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().get(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isEmpty() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().isEmpty();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().isEmpty();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<K> keySet() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().keySet();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().keySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V put(K key, V value) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().put(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().put(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().putAll(map);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().putAll(map);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V remove(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().remove(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().remove(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int size() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().size();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().size();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Collection<V> values() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().values();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().values();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().getNodesWithKeys(portableKeys);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().getNodesWithKeys(portableKeys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().unlockedRemoveNoReturn(k);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().unlockedRemoveNoReturn(k);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().unlockedGet(k, quiet);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().unlockedGet(k, quiet);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void clearLocalCache() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().clearLocalCache();
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().clearLocalCache();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V unsafeLocalGet(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().unsafeLocalGet(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().unsafeLocalGet(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsLocalKey(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().containsLocalKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().containsLocalKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localSize() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localSize();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<K> localKeySet() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localKeySet();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localKeySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public long localOnHeapSizeInBytes() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localOnHeapSizeInBytes();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localOnHeapSizeInBytes();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public long localOffHeapSizeInBytes() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localOffHeapSizeInBytes();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localOffHeapSizeInBytes();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localOnHeapSize() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localOnHeapSize();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localOnHeapSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localOffHeapSize() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().localOffHeapSize();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().localOffHeapSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().containsKeyLocalOnHeap(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().containsKeyLocalOnHeap(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().containsKeyLocalOffHeap(key);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().containsKeyLocalOffHeap(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      return getDelegate().put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void disposeLocally() {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().disposeLocally();
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().disposeLocally();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    nonStopManager.begin(getTimeout(getMethod()));

    try {
      getDelegate().removeAll(keys);
    } catch (ToolkitAbortableOperationException e) {
      resolveTimeoutBehavior().removeAll(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    nonStopManager.begin(getTimeout(getMethod()));
    try {
      return ((ValuesResolver<K, V>) getDelegate()).get(key, valueOid);
    } catch (ToolkitAbortableOperationException e) {
      return ((ValuesResolver<K, V>) resolveTimeoutBehavior()).get(key, valueOid);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    nonStopManager.begin(getTimeout(getMethod()));
    try {
      return getDelegate().createQueryBuilder();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().createQueryBuilder();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public SearchExecutor createSearchExecutor() {
    nonStopManager.begin(getTimeout(getMethod()));
    try {
      return getDelegate().createSearchExecutor();
    } catch (ToolkitAbortableOperationException e) {
      return resolveTimeoutBehavior().createSearchExecutor();
    } finally {
      nonStopManager.finish();
    }
  }

}
