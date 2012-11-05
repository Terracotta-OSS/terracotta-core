/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SearchExecutor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.object.DestroyableToolkitObject;
import com.terracotta.toolkit.type.DistributedToolkitType;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class LocalReadsToolkitCacheImpl<K, V> implements DistributedToolkitType<InternalToolkitMap<K, V>>,
    ValuesResolver<K, V>, ToolkitCacheInternal<K, V>, DestroyableToolkitObject {
  private final ToolkitCacheInternal<K, V> delegate;
  private final ToolkitCacheInternal<K, V> mutationBehaviourResolver;

  public LocalReadsToolkitCacheImpl(ToolkitCacheInternal<K, V> delegate,
                                    ToolkitCacheInternal<K, V> mutationBehaviourResolver) {
    this.delegate = delegate;
    this.mutationBehaviourResolver = mutationBehaviourResolver;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public boolean isDestroyed() {
    return delegate.isDestroyed();
  }

  @Override
  public void destroy() {
    mutationBehaviourResolver.destroy();
  }

  @Override
  public V getQuiet(Object key) {
    return delegate.unsafeLocalGet(key);
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
    mutationBehaviourResolver.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);

  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    return mutationBehaviourResolver.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    mutationBehaviourResolver.addListener(listener);
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    mutationBehaviourResolver.removeListener(listener);
  }

  @Override
  public void unpinAll() {
    delegate.unpinAll();
  }

  @Override
  public boolean isPinned(K key) {
    return delegate.isPinned(key);
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    // TODO: discuss
    delegate.setPinned(key, pinned);
  }

  @Override
  public void removeNoReturn(Object key) {
    mutationBehaviourResolver.removeNoReturn(key);
  }

  @Override
  public void putNoReturn(K key, V value) {
    mutationBehaviourResolver.putNoReturn(key, value);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return getAllQuiet((Collection<K>) keys);
  }

  @Override
  public Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    mutationBehaviourResolver.setConfigField(name, value);

  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    // TODO: return nonstop lock when supporting nonstop for locks.
    return delegate.createLockForKey(key);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    delegate.setAttributeExtractor(attrExtractor);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return mutationBehaviourResolver.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return mutationBehaviourResolver.remove(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return mutationBehaviourResolver.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return mutationBehaviourResolver.replace(key, value);
  }

  @Override
  public int size() {
    return delegate.localSize();
  }

  @Override
  public boolean isEmpty() {
    return delegate.localSize() == 0;
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
    return mutationBehaviourResolver.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return mutationBehaviourResolver.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    mutationBehaviourResolver.putAll(m);
  }

  @Override
  public void clear() {
    mutationBehaviourResolver.clear();
  }

  @Override
  public Set<K> keySet() {
    return delegate.localKeySet();
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
    Map<K, V> allValuesMap = new HashMap<K, V>(delegate.localSize());
    for (K key : delegate.keySet()) {
      allValuesMap.put(key, getQuiet(key));
    }
    return allValuesMap;
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    return mutationBehaviourResolver.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    mutationBehaviourResolver.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);

  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    mutationBehaviourResolver.unlockedRemoveNoReturn(k);

  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return getQuiet(k);
  }

  @Override
  public void clearLocalCache() {
    // TODO: discuss
    mutationBehaviourResolver.clearLocalCache();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return delegate.unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return delegate.containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return delegate.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return delegate.localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return delegate.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return delegate.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return delegate.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return delegate.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return delegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return delegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return mutationBehaviourResolver.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void disposeLocally() {
    // TODO: discuss
    mutationBehaviourResolver.disposeLocally();
  }

  @Override
  public void removeAll(Set<K> keys) {
    mutationBehaviourResolver.removeAll(keys);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return delegate.createQueryBuilder();
  }

  @Override
  public SearchExecutor createSearchExecutor() {
    return delegate.createSearchExecutor();
  }

  @Override
  public Iterator<InternalToolkitMap<K, V>> iterator() {
    return ((DistributedToolkitType<InternalToolkitMap<K, V>>) delegate).iterator();
  }

  @Override
  public void doDestroy() {
    ((DestroyableToolkitObject) mutationBehaviourResolver).doDestroy();
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    // TODO: discuss change in behavior for search here.
    return delegate.unsafeLocalGet(key);
  }
}
