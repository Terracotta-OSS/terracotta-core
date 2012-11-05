/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.SearchExecutor;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.collections.DestroyedInstanceProxy;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.type.DistributedToolkitType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ToolkitCacheImpl<K, V> extends AbstractDestroyableToolkitObject implements
    DistributedToolkitType<InternalToolkitMap<K, V>>, ValuesResolver<K, V>,
    ToolkitCacheInternal<K, V> {

  private final AggregateServerMap<K, V>      aggregateServerMap;
  private volatile ToolkitCacheInternal<K, V> activeDelegate;
  private final String                        name;

  public ToolkitCacheImpl(ToolkitObjectFactory factory, ToolkitInternal toolkit, String name,
                          AggregateServerMap<K, V> delegate) {
    super(factory);
    this.name = name;
    this.aggregateServerMap = delegate;
    activeDelegate = aggregateServerMap;
    this.aggregateServerMap.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void applyDestroy() {
    this.activeDelegate = DestroyedInstanceProxy.createNewInstance(ToolkitCacheInternal.class, getName());
  }

  @Override
  public void doDestroy() {
    activeDelegate.destroy();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SearchExecutor createSearchExecutor() {
    return activeDelegate.createSearchExecutor();
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return activeDelegate.createQueryBuilder();
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return activeDelegate.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return activeDelegate.remove(key, value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return activeDelegate.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return activeDelegate.replace(key, value);
  }

  @Override
  public int size() {
    return activeDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return activeDelegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return activeDelegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return activeDelegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return activeDelegate.get(key);
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    return ((ValuesResolver<K, V>) activeDelegate).get(key, valueOid);
  }

  @Override
  public V put(K key, V value) {
    return activeDelegate.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return activeDelegate.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    activeDelegate.putAll(m);
  }

  @Override
  public void clear() {
    activeDelegate.clear();
  }

  @Override
  public Set<K> keySet() {
    return activeDelegate.keySet();
  }

  @Override
  public Collection<V> values() {
    return activeDelegate.values();
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return activeDelegate.entrySet();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return activeDelegate.createLockForKey(key);
  }

  @Override
  public void removeNoReturn(Object key) {
    activeDelegate.removeNoReturn(key);
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return activeDelegate.unsafeLocalGet(key);
  }

  @Override
  public void putNoReturn(K key, V value) {
    activeDelegate.putNoReturn(key, value);
  }

  @Override
  public int localSize() {
    return activeDelegate.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return activeDelegate.localKeySet();
  }

  @Override
  public void unpinAll() {
    activeDelegate.unpinAll();
  }

  @Override
  public boolean isPinned(K key) {
    return activeDelegate.isPinned(key);
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    activeDelegate.setPinned(key, pinned);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return activeDelegate.containsLocalKey(key);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return activeDelegate.getAll(keys);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    return activeDelegate.getAllQuiet(keys);
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    activeDelegate.addListener(listener);
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    activeDelegate.removeListener(listener);
  }

  @Override
  public Configuration getConfiguration() {
    return activeDelegate.getConfiguration();
  }

  @Override
  public V getQuiet(Object key) {
    return activeDelegate.getQuiet(key);
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    activeDelegate.setConfigField(name, value);
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    activeDelegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    return activeDelegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return activeDelegate.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return activeDelegate.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return activeDelegate.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return activeDelegate.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return activeDelegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return activeDelegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public void disposeLocally() {
    activeDelegate.disposeLocally();
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    return activeDelegate.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    activeDelegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    activeDelegate.unlockedRemoveNoReturn(k);
  }

  @Override
  public void clearLocalCache() {
    activeDelegate.clearLocalCache();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return activeDelegate.unlockedGet(k, quiet);
  }

  @Override
  public Iterator<InternalToolkitMap<K, V>> iterator() {
    return aggregateServerMap.iterator();
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor extractor) {
    activeDelegate.setAttributeExtractor(extractor);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return activeDelegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void removeAll(Set<K> keys) {
    activeDelegate.removeAll(keys);

  }

}
