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
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.bulkload.BulkLoadToolkitCache;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ToolkitCacheImpl<K, V> extends AbstractDestroyableToolkitObject implements
    DistributedToolkitType<InternalToolkitMap<K, V>>, ValuesResolver<K, V>, ToolkitCacheImplInterface<K, V>,
    OnGCCallable {

  // private static final TCLogger LOGGER = TCLogging.getLogger(ToolkitCacheImpl.class);
  private volatile AggregateServerMap<K, V>   aggregateServerMap;
  private volatile ToolkitCacheInternal<K, V> activeDelegate;
  private volatile ToolkitCacheInternal<K, V> localDelegate;
  private volatile ToolkitCacheInternal<K, V> currentDelegate;
  private final BulkLoadToolkitCache<K, V>    bulkloadCache;
  private final String                        name;
  private final OnGCCallable                  onGCCallable;
  private final AtomicBoolean                 delegateSwitched = new AtomicBoolean(false);
  // lock used to protect the state of activeDelegate and localDelegate.
  private final ReadWriteLock                 lock = new ReentrantReadWriteLock();

  public ToolkitCacheImpl(ToolkitObjectFactory factory, String name, AggregateServerMap<K, V> delegate,
                          PlatformService platformService, ToolkitInternal toolkit) {
    super(factory);
    this.name = name;
    this.aggregateServerMap = delegate;
    this.activeDelegate = aggregateServerMap;
    this.localDelegate = aggregateServerMap;
    this.bulkloadCache = new BulkLoadToolkitCache<K, V>(platformService, name, aggregateServerMap, toolkit);
    this.aggregateServerMap.setApplyDestroyCallback(getDestroyApplicator());
    this.onGCCallable = new OnGCCallable(aggregateServerMap);
  }

  private void readLock() {
    lock.readLock().lock();
  }

  private void readUnlock() {
    lock.readLock().unlock();
  }

  private void writeLock() {
    lock.writeLock().lock();
  }

  private void writeUnlock() {
    lock.writeLock().unlock();
  }

  @Override
  public void doRejoinStarted() {
    writeLock();
    try {
      if (!delegateSwitched.get()) {
        this.currentDelegate = this.activeDelegate;
        this.activeDelegate = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitCacheImplInterface.class);
        delegateSwitched.set(true);
      }
      aggregateServerMap.rejoinStarted();
      bulkloadCache.rejoinCleanUp();
    } finally {
      writeUnlock();
    }
  }

  @Override
  public void doRejoinCompleted() {
    writeLock();
    try {
      aggregateServerMap.rejoinCompleted();
      aggregateServerMap.setApplyDestroyCallback(getDestroyApplicator());
      bulkloadCache.rejoinCompleted();
      if (aggregateServerMap.isLookupSuccessfulAfterRejoin()) {
        this.activeDelegate = this.currentDelegate;
        delegateSwitched.set(false);
      } else {
        destroyApplicator.applyDestroy();
      }
    } finally {
      writeUnlock();
    }

  }

  @Override
  public void applyDestroy() {
    writeLock();
    try {
      // status.setDestroyed() is called from Parent class
      ToolkitCacheImplInterface destroyedInstanceProxy = ToolkitInstanceProxy
          .newDestroyedInstanceProxy(getName(), ToolkitCacheImplInterface.class);
      this.aggregateServerMap = null;
      this.activeDelegate = destroyedInstanceProxy;
      this.localDelegate = destroyedInstanceProxy;
    } finally {
      writeUnlock();
    }
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
    readLock();
    try {
      return localDelegate.createQueryBuilder();
    } finally {
      readUnlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    readLock();
    try {
      return activeDelegate.putIfAbsent(key, value);
    } finally {
      readUnlock();
    }

  }

  @Override
  public boolean remove(Object key, Object value) {
    readLock();
    try {
      return activeDelegate.remove(key, value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    readLock();
    try {
      return activeDelegate.replace(key, oldValue, newValue);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V replace(K key, V value) {
    readLock();
    try {
      return activeDelegate.replace(key, value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public int size() {
    readLock();
    try {
      return activeDelegate.size();
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean isEmpty() {
    readLock();
    try {
      return activeDelegate.isEmpty();
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    readLock();
    try {
      return activeDelegate.containsKey(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    readLock();
    try {
      return activeDelegate.containsValue(value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V get(Object key) {
    readLock();
    try {
      return activeDelegate.get(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    readLock();
    try {
      return ((ValuesResolver<K, V>) activeDelegate).get(key, valueOid);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V put(K key, V value) {
    readLock();
    try {
      return activeDelegate.put(key, value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V remove(Object key) {
    readLock();
    try {
      return activeDelegate.remove(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    readLock();
    try {
      activeDelegate.putAll(m);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void clear() {
    readLock();
    try {
      activeDelegate.clear();
    } finally {
      readUnlock();
    }
  }

  @Override
  public void clearVersioned() {
    readLock();
    try {
      activeDelegate.clearVersioned();
    } finally {
      readUnlock();
    }
  }

  @Override
  public Set<K> keySet() {
    readLock();
    try {
      return new SubTypeWrapperSet(activeDelegate.keySet(), status, this.name, ToolkitObjectType.CACHE);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Collection<V> values() {
    readLock();
    try {
      return new SubTypeWrapperCollection<V>(activeDelegate.values(), status, this.name, ToolkitObjectType.CACHE);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    readLock();
    try {
      return new SubTypeWrapperSet(activeDelegate.entrySet(), status, this.name, ToolkitObjectType.CACHE);
    } finally {
      readUnlock();
    }
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    readLock();
    try {
      return activeDelegate.createLockForKey(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void removeNoReturn(Object key) {
    readLock();
    try {
      activeDelegate.removeNoReturn(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V unsafeLocalGet(Object key) {
    readLock();
    try {
      return localDelegate.unsafeLocalGet(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putNoReturn(K key, V value) {
    readLock();
    try {
      activeDelegate.putNoReturn(key, value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public int localSize() {
    readLock();
    try {
      return localDelegate.localSize();
    } finally {
      readUnlock();
    }
  }

  @Override
  public Set<K> localKeySet() {
    readLock();
    try {
      return localDelegate.localKeySet();
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsLocalKey(Object key) {
    readLock();
    try {
      return localDelegate.containsLocalKey(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    readLock();
    try {
      return new SubTypeWrapperMap<K, V>(activeDelegate.getAll(keys), status, this.name, ToolkitObjectType.CACHE);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    readLock();
    try {
      return new SubTypeWrapperMap<K, V>(activeDelegate.getAllQuiet(keys), status, this.name, ToolkitObjectType.CACHE);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    readLock();
    try {
      activeDelegate.addListener(listener);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    readLock();
    try {
      activeDelegate.removeListener(listener);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Configuration getConfiguration() {
    readLock();
    try {
      return activeDelegate.getConfiguration();
    } finally {
      readUnlock();
    }
  }

  @Override
  public V getQuiet(Object key) {
    readLock();
    try {
      return activeDelegate.getQuiet(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    readLock();
    try {
      activeDelegate.setConfigField(name, value);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    readLock();
    try {
      activeDelegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    readLock();
    try {
      return activeDelegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      readUnlock();
    }
  }

  @Override
  public long localOnHeapSizeInBytes() {
    readLock();
    try {
      return localDelegate.localOnHeapSizeInBytes();
    } finally {
      readUnlock();
    }
  }

  @Override
  public long localOffHeapSizeInBytes() {
    readLock();
    try {
      return localDelegate.localOffHeapSizeInBytes();
    } finally {
      readUnlock();
    }
  }

  @Override
  public int localOnHeapSize() {
    readLock();
    try {
      return localDelegate.localOnHeapSize();
    } finally {
      readUnlock();
    }
  }

  @Override
  public int localOffHeapSize() {
    readLock();
    try {
      return localDelegate.localOffHeapSize();
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    readLock();
    try {
      return localDelegate.containsKeyLocalOnHeap(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    readLock();
    try {
      return localDelegate.containsKeyLocalOffHeap(key);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void disposeLocally() {
    writeLock();
    try {
      bulkloadCache.disposeLocally();
      // do not use applicator as that will destroy the object in cluster also.
      applyDestroy();
      factory.dispose(this);
    } finally {
      writeUnlock();
    }
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    readLock();
    try {
      return activeDelegate.getNodesWithKeys(portableKeys);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    readLock();
    try {
      activeDelegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    activeDelegate.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    readLock();
    try {
      activeDelegate.unlockedRemoveNoReturn(k);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    activeDelegate.unlockedRemoveNoReturnVersioned(key, version);
  }

  @Override
  public void clearLocalCache() {
    readLock();
    try {
      localDelegate.clearLocalCache();
    } finally {
      readUnlock();
    }
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    readLock();
    try {
      return activeDelegate.unlockedGet(k, quiet);
    } finally {
      readUnlock();
    }
  }

  @Override
  public Iterator<InternalToolkitMap<K, V>> iterator() {
    readLock();
    try {
      return aggregateServerMap.iterator();
    } finally {
      readUnlock();
    }
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor extractor) {
    readLock();
    try {
      activeDelegate.setAttributeExtractor(extractor);
    } finally {
      readUnlock();
    }
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    readLock();
    try {
      return activeDelegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    readLock();
    try {
      activeDelegate.putVersioned(key, value, version);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version) {
    readLock();
    try {
      activeDelegate.putIfAbsentVersioned(key, value, version);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version, int createTimeInSecs,
                         int customMaxTTISeconds, int customMaxTTLSeconds) {
    readLock();
    try {
      activeDelegate.putIfAbsentVersioned(key, value, version, createTimeInSecs,
                         customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      readUnlock();
    }

  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    readLock();
    try {
      activeDelegate.putVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    readLock();
    try {
      activeDelegate.removeAll(keys);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    readLock();
    try {
      activeDelegate.removeVersioned(key, version);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    activeDelegate.registerVersionUpdateListener(listener);
  }

  @Override
  public void unregisterVersionUpdateListener(final VersionUpdateListener listener) {
    activeDelegate.unregisterVersionUpdateListener(listener);
  }

  @Override
  public Set<K> keySetForSegment(final int segmentIndex) {
    return activeDelegate.keySetForSegment(segmentIndex);
  }

  @Override
  public VersionedValue<V> getVersionedValue(Object key) {
    return activeDelegate.getVersionedValue(key);
  }

  public Map<K, VersionedValue<V>> getAllVersioned(Collection<K> key) {
    return activeDelegate.getAllVersioned(key);
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    readLock();
    try {
      return activeDelegate.unlockedGetAll(keys, quiet);
    } finally {
      readUnlock();
    }
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

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    lock.writeLock().lock();
    try {
      if (enabledBulkLoad && !isNodeBulkLoadEnabled()) {
        this.activeDelegate = bulkloadCache;
        this.localDelegate = bulkloadCache;
        this.bulkloadCache.setNodeBulkLoadEnabled(true);
      }

      if (!enabledBulkLoad && isNodeBulkLoadEnabled()) {
        this.activeDelegate = aggregateServerMap;
        this.localDelegate = aggregateServerMap;
        this.bulkloadCache.setNodeBulkLoadEnabled(false);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    bulkloadCache.waitUntilBulkLoadComplete();
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return bulkloadCache.isBulkLoadEnabled();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return bulkloadCache.isNodeBulkLoadEnabled();
  }

  @Override
  public void quickClear() {
    readLock();
    try {
      activeDelegate.quickClear();
    } finally {
      readUnlock();
    }
  }

  @Override
  public int quickSize() {
    readLock();
    try {
      return activeDelegate.quickSize();
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean remove(Object key, Object value, ToolkitValueComparator<V> comparator) {
    readLock();
    try {
      return activeDelegate.remove(key, value, comparator);
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue, ToolkitValueComparator<V> comparator) {
    readLock();
    try {
      return activeDelegate.replace(key, oldValue, newValue, comparator);
    } finally {
      readUnlock();
    }
  }

  @Override
  public void startBuffering() {
    lock.writeLock().lock();
    try {
      if (!bulkloadCache.isBuffering()) {
        this.activeDelegate = bulkloadCache;
        this.localDelegate = bulkloadCache;
        this.bulkloadCache.startBuffering();
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void stopBuffering() {
    lock.writeLock().lock();
    try {
      if (bulkloadCache.isBuffering()) {
        this.activeDelegate = aggregateServerMap;
        this.localDelegate = aggregateServerMap;
        this.bulkloadCache.stopBuffering();
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void flushBuffer() {
    readLock();
    try {
      if (bulkloadCache.isBuffering()) {
        bulkloadCache.flushBuffer();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public boolean isBuffering() {
    return bulkloadCache.isBuffering();
  }
}
