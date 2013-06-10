/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.type.DistributedToolkitType;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;
import com.terracotta.toolkit.util.collections.OnGCCallable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class ToolkitCacheImpl<K, V> extends AbstractDestroyableToolkitObject implements
    DistributedToolkitType<InternalToolkitMap<K, V>>, ValuesResolver<K, V>, ToolkitCacheImplInterface<K, V>,
    OnGCCallable {

  private volatile AggregateServerMap<K, V>        aggregateServerMap;
  private volatile ToolkitCacheImplInterface<K, V> activeDelegate;
  private volatile ToolkitCacheImplInterface<K, V> localDelegate;
  private final String                             name;
  private volatile OnGCCallable                    onGCCallable;

  public ToolkitCacheImpl(ToolkitObjectFactory factory, String name, AggregateServerMap<K, V> delegate) {
    super(factory);
    this.name = name;
    this.aggregateServerMap = delegate;
    this.activeDelegate = aggregateServerMap;
    this.localDelegate = aggregateServerMap;
    this.aggregateServerMap.setApplyDestroyCallback(getDestroyApplicator());
    this.onGCCallable = new OnGCCallable(aggregateServerMap);
  }

  @Override
  public void doRejoinStarted() {
    this.activeDelegate = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitCacheImplInterface.class);
    aggregateServerMap.rejoinStarted();
  }

  @Override
  public void doRejoinCompleted() {
    aggregateServerMap.rejoinCompleted();
    aggregateServerMap.setApplyDestroyCallback(getDestroyApplicator());
    if (aggregateServerMap.isLookupSuccessfulAfterRejoin()) {
      this.activeDelegate = aggregateServerMap;
    } else {
      destroyApplicator.applyDestroy();
    }
  }

  @Override
  public void applyDestroy() {
    // status.setDestroyed() is called from Parent class
    this.activeDelegate = ToolkitInstanceProxy.newDestroyedInstanceProxy(getName(), ToolkitCacheImplInterface.class);
    this.localDelegate = ToolkitInstanceProxy.newDestroyedInstanceProxy(getName(), ToolkitCacheImplInterface.class);
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
    return new SubTypeWrapperSet(activeDelegate.keySet(), status, this.name, ToolkitObjectType.CACHE);
  }

  @Override
  public Collection<V> values() {
    return new SubTypeWrapperCollection<V>(activeDelegate.values(), status, this.name, ToolkitObjectType.CACHE);
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return new SubTypeWrapperSet(activeDelegate.entrySet(), status, this.name, ToolkitObjectType.CACHE);
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
    return localDelegate.unsafeLocalGet(key);
  }

  @Override
  public void putNoReturn(K key, V value) {
    activeDelegate.putNoReturn(key, value);
  }

  @Override
  public int localSize() {
    return localDelegate.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return localDelegate.localKeySet();
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return localDelegate.containsLocalKey(key);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return new SubTypeWrapperMap<K, V>(activeDelegate.getAll(keys), status, this.name, ToolkitObjectType.CACHE);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    return new SubTypeWrapperMap<K, V>(activeDelegate.getAllQuiet(keys), status, this.name, ToolkitObjectType.CACHE);
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    // TODO : handle rejoin ...
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
    return localDelegate.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return localDelegate.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return localDelegate.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return localDelegate.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return localDelegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return localDelegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public void disposeLocally() {
    localDelegate.disposeLocally();
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
    localDelegate.clearLocalCache();
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

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return activeDelegate.unlockedGetAll(keys, quiet);
  }

  @Override
  public Callable<Void> onGCCallable() {
    return onGCCallable;
  }

  private static class OnGCCallable implements Callable<Void> {
    private final TCObjectServerMap objectServerMap;

    public OnGCCallable(AggregateServerMap aggregateServerMap) {
      objectServerMap = ((TCObjectServerMap) aggregateServerMap.getAnyServerMap().__tc_managed());
    }

    @Override
    public Void call() throws Exception {
      objectServerMap.clearAllLocalCacheInline();
      return null;
    }
  }
}
