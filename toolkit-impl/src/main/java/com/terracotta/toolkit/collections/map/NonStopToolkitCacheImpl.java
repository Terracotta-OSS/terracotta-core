/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.search.SearchBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfig;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import com.tc.object.ObjectID;
import com.terracotta.toolkit.abortable.NonStopManager;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.type.DistributedToolkitType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NonStopToolkitCacheImpl<K, V> implements DistributedToolkitType<InternalToolkitMap<K, V>>,
    ValuesResolver<K, V>, ToolkitCacheInternal<K, V> {

  public static interface ToolkitCacheInterface<X, Y> extends DistributedToolkitType<InternalToolkitMap<X, Y>>,
      ValuesResolver<X, Y>, ToolkitCacheInternal<X, Y> {
    //
  }

  private final NonStopManager              nonStopManager;
  private final ToolkitCacheInterface<K, V> delegate;
  private final ToolkitCacheInterface<K, V> timeoutResolver;
  private final NonStopConfig               nonStopConfig;

  public NonStopToolkitCacheImpl(NonStopManager nonStopManager, ToolkitCacheInterface<K, V> delegate,
                                 ToolkitCacheInterface<K, V> timeoutResolver, NonStopConfig nonStopConfig) {
    this.nonStopManager = nonStopManager;
    this.delegate = delegate;
    this.timeoutResolver = timeoutResolver;
    this.nonStopConfig = nonStopConfig;
  }

  @Override
  public String getName() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getName();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getName();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isDestroyed() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.isDestroyed();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.isDestroyed();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void destroy() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.destroy();
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.destroy();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Iterator<InternalToolkitMap<K, V>> iterator() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.iterator();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.iterator();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V getQuiet(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getQuiet(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getQuiet(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getAllQuiet(keys);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getAllQuiet(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.addListener(listener);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.addListener(listener);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.removeListener(listener);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.removeListener(listener);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unpinAll() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.unpinAll();
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.unpinAll();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isPinned(K key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.isPinned(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.isPinned(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.setPinned(key, pinned);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.setPinned(key, pinned);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeNoReturn(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.removeNoReturn(key);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.removeNoReturn(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putNoReturn(K key, V value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.putNoReturn(key, value);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.putNoReturn(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getAll(keys);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getAll(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Configuration getConfiguration() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getConfiguration();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getConfiguration();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.setConfigField(name, value);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.setConfigField(name, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsValue(Object value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.containsValue(value);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.containsValue(value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.createLockForKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.createLockForKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.setAttributeExtractor(attrExtractor);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.setAttributeExtractor(attrExtractor);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.putIfAbsent(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.putIfAbsent(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean remove(Object key, Object value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.remove(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.remove(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V replace(K key, V value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.replace(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.replace(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.replace(key, oldValue, newValue);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.replace(key, oldValue, newValue);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void clear() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.clear();
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.clear();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.containsKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.containsKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.entrySet();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.entrySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V get(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.get(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.get(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean isEmpty() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.isEmpty();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.isEmpty();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<K> keySet() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.keySet();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.keySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V put(K key, V value) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.put(key, value);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.put(key, value);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.putAll(map);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.putAll(map);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V remove(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.remove(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.remove(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int size() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.size();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.size();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Collection<V> values() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.values();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.values();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.getNodesWithKeys(portableKeys);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.getNodesWithKeys(portableKeys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.unlockedRemoveNoReturn(k);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.unlockedRemoveNoReturn(k);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.unlockedGet(k, quiet);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.unlockedGet(k, quiet);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void clearLocalCache() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.clearLocalCache();
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.clearLocalCache();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V unsafeLocalGet(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.unsafeLocalGet(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.unsafeLocalGet(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsLocalKey(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.containsLocalKey(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.containsLocalKey(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localSize() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localSize();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public Set<K> localKeySet() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localKeySet();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localKeySet();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public long localOnHeapSizeInBytes() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localOnHeapSizeInBytes();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localOnHeapSizeInBytes();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public long localOffHeapSizeInBytes() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localOffHeapSizeInBytes();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localOffHeapSizeInBytes();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localOnHeapSize() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localOnHeapSize();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localOnHeapSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public int localOffHeapSize() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.localOffHeapSize();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.localOffHeapSize();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.containsKeyLocalOnHeap(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.containsKeyLocalOnHeap(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.containsKeyLocalOffHeap(key);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.containsKeyLocalOffHeap(key);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void disposeLocally() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.disposeLocally();
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.disposeLocally();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      delegate.removeAll(keys);
    } catch (ToolkitAbortableOperationException e) {
      timeoutResolver.removeAll(keys);
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public SearchBuilder createSearchBuilder() {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.createSearchBuilder();
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.createSearchBuilder();
    } finally {
      nonStopManager.finish();
    }
  }

  @Override
  public V get(K key, ObjectID valueOid) {
    nonStopManager.begin(nonStopConfig.getTimeout());

    try {
      return delegate.get(key, valueOid);
    } catch (ToolkitAbortableOperationException e) {
      return timeoutResolver.get(key, valueOid);
    } finally {
      nonStopManager.finish();
    }
  }

}
