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

import com.terracotta.toolkit.object.DestroyableToolkitObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class TimeoutBehaviorToolkitCacheImpl<K, V> implements ToolkitCacheImplInterface<K, V>,
    DestroyableToolkitObject {
  private final ToolkitCacheImplInterface<K, V> mutationBehaviourResolver;
  private final ToolkitCacheImplInterface<K, V> immutationBehaviourResolver;

  public TimeoutBehaviorToolkitCacheImpl(ToolkitCacheImplInterface<K, V> immutationBehaviourResolver,
                                         ToolkitCacheImplInterface<K, V> mutationBehaviourResolver) {
    this.immutationBehaviourResolver = immutationBehaviourResolver;
    this.mutationBehaviourResolver = mutationBehaviourResolver;
  }

  @Override
  public String getName() {
    return immutationBehaviourResolver.getName();
  }

  @Override
  public boolean isDestroyed() {
    return immutationBehaviourResolver.isDestroyed();
  }

  @Override
  public void destroy() {
    mutationBehaviourResolver.destroy();
  }

  @Override
  public V getQuiet(Object key) {
    return immutationBehaviourResolver.getQuiet(key);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    return immutationBehaviourResolver.getAllQuiet(keys);
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
  public void removeNoReturn(Object key) {
    mutationBehaviourResolver.removeNoReturn(key);
  }

  @Override
  public void putNoReturn(K key, V value) {
    mutationBehaviourResolver.putNoReturn(key, value);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return immutationBehaviourResolver.getAll(keys);
  }

  @Override
  public Configuration getConfiguration() {
    return immutationBehaviourResolver.getConfiguration();
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
    return immutationBehaviourResolver.createLockForKey(key);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    immutationBehaviourResolver.setAttributeExtractor(attrExtractor);
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
    return immutationBehaviourResolver.size();
  }

  @Override
  public boolean isEmpty() {
    return immutationBehaviourResolver.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return immutationBehaviourResolver.containsKey(key);
  }

  @Override
  public V get(Object key) {
    return immutationBehaviourResolver.get(key);
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
    return immutationBehaviourResolver.keySet();
  }

  @Override
  public Collection<V> values() {
    return immutationBehaviourResolver.values();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return immutationBehaviourResolver.entrySet();
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
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime, final int customTTI, final int customTTL) {
    mutationBehaviourResolver.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    mutationBehaviourResolver.unlockedRemoveNoReturn(k);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    mutationBehaviourResolver.unlockedRemoveNoReturnVersioned(key, version);
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return immutationBehaviourResolver.unlockedGet(k, quiet);
  }

  @Override
  public void clearLocalCache() {
    mutationBehaviourResolver.clearLocalCache();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return immutationBehaviourResolver.unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return immutationBehaviourResolver.containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return immutationBehaviourResolver.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return immutationBehaviourResolver.localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return immutationBehaviourResolver.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return immutationBehaviourResolver.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return immutationBehaviourResolver.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return immutationBehaviourResolver.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return immutationBehaviourResolver.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return immutationBehaviourResolver.containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return mutationBehaviourResolver.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    mutationBehaviourResolver.putVersioned(key, value, version);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    mutationBehaviourResolver.putVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
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
  public void removeVersioned(final Object key, final long version) {
    mutationBehaviourResolver.removeVersioned(key, version);
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    mutationBehaviourResolver.registerVersionUpdateListener(listener);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return immutationBehaviourResolver.createQueryBuilder();
  }

  @Override
  public void doDestroy() {
    ((DestroyableToolkitObject) mutationBehaviourResolver).doDestroy();
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return immutationBehaviourResolver.unlockedGetAll(keys, quiet);
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return immutationBehaviourResolver.isBulkLoadEnabled();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return immutationBehaviourResolver.isNodeBulkLoadEnabled();
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    mutationBehaviourResolver.setNodeBulkLoadEnabled(enabledBulkLoad);

  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    immutationBehaviourResolver.waitUntilBulkLoadComplete();
  }
}
