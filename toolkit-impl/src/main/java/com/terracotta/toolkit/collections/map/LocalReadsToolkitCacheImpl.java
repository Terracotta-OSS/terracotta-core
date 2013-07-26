/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.nonstop.ToolkitObjectLookup;
import com.terracotta.toolkit.object.DestroyableToolkitObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalReadsToolkitCacheImpl<K, V> implements ValuesResolver<K, V>, ToolkitCacheImplInterface<K, V>,
    DestroyableToolkitObject, ToolkitStore<K, V> {
  private final ToolkitObjectLookup<ToolkitCacheImplInterface<K, V>> delegate;
  private final ToolkitCacheImplInterface<K, V>                      noOpBehaviourResolver;

  public LocalReadsToolkitCacheImpl(ToolkitObjectLookup<ToolkitCacheImplInterface<K, V>> delegate,
                                    ToolkitCacheImplInterface<K, V> noOpBehaviourResolver) {
    this.delegate = delegate;
    this.noOpBehaviourResolver = noOpBehaviourResolver;
  }

  private ToolkitCacheImplInterface<K, V> getDelegate() {
    ToolkitCacheImplInterface<K, V> rv = delegate.getInitializedObjectOrNull();
    if (rv == null) { return noOpBehaviourResolver; }
    return rv;
  }

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public boolean isDestroyed() {
    return getDelegate().isDestroyed();
  }

  @Override
  public void destroy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V getQuiet(Object key) {
    return getDelegate().unsafeLocalGet(key);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, getQuiet(key));
    }
    return rv;
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeNoReturn(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putNoReturn(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return getAllQuiet((Collection<K>) keys);
  }

  @Override
  public Configuration getConfiguration() {
    return getDelegate().getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    // TODO: return nonstop lock when supporting nonstop for locks.
    return getDelegate().createLockForKey(key);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    getDelegate().setAttributeExtractor(attrExtractor);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return getDelegate().localSize();
  }

  @Override
  public boolean isEmpty() {
    return getDelegate().localSize() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return containsLocalKey(key);
  }

  @Override
  public V get(Object key) {
    return getQuiet(key);
  }

  @Override
  public V put(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<K> keySet() {
    return getDelegate().localKeySet();
  }

  @Override
  public Collection<V> values() {
    Map<K, V> allValuesMap = getAllLocalKeyValuesMap();
    return allValuesMap.values();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    Map<K, V> allValuesMap = getAllLocalKeyValuesMap();
    return allValuesMap.entrySet();
  }

  private Map<K, V> getAllLocalKeyValuesMap() {
    Map<K, V> allValuesMap = new HashMap<K, V>(getDelegate().localSize());
    for (K key : getDelegate().keySet()) {
      allValuesMap.put(key, getQuiet(key));
    }
    return allValuesMap;
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime, final int customTTI, final int customTTL) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return getQuiet(k);
  }

  @Override
  public void clearLocalCache() {
    // TODO: discuss
    throw new UnsupportedOperationException();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return getDelegate().unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return getDelegate().containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return getDelegate().localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return getDelegate().localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return getDelegate().localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return getDelegate().localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return getDelegate().localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return getDelegate().localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return getDelegate().containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return getDelegate().containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void disposeLocally() {
    // TODO: discuss
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAll(Set<K> keys) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return getDelegate().createQueryBuilder();
  }

  @Override
  public void doDestroy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    return null;
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return getDelegate().unlockedGetAll(keys, quiet);
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return false;
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return false;
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    // do nothing
  }
}
