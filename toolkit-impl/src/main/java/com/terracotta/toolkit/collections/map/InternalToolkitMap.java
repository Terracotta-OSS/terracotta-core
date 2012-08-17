/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.cache.ToolkitCacheMetaDataCallback;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.meta.MetaData;

import com.tc.object.bytecode.TCServerMap;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.terracotta.toolkit.collections.map.ServerMap.GetType;
import com.terracotta.toolkit.object.TCToolkitObject;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public interface InternalToolkitMap<K, V> extends ConcurrentMap<K, V>, TCServerMap, TCToolkitObject,
    ValuesResolver<K, V> {

  String getName();

  ToolkitLockTypeInternal getLockType();

  boolean isEventual();

  boolean isLocalCacheEnabled();

  int getMaxTTISeconds();

  int getMaxTTLSeconds();

  int getMaxCountInCluster();

  V putWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                    MetaData metaData);

  void putNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                               MetaData metaData);

  V putIfAbsentWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                            MetaData metaData);

  V get(Object key, boolean quiet);

  void addCacheListener(ToolkitCacheListener<K> listener);

  void removeCacheListener(ToolkitCacheListener<K> listener);

  void setConfigField(String name, Object value);

  void setMetaDataCallback(ToolkitCacheMetaDataCallback callback);

  void initializeLocalCache(L1ServerMapLocalCacheStore<K, V> localCacheStore);

  void removeNoReturnWithMetaData(Object key, MetaData metaData);

  V removeWithMetaData(Object key, MetaData metaData);

  boolean removeWithMetaData(Object key, Object value, MetaData metaData);

  V unsafeLocalGet(Object key);

  V unlockedGet(K key, boolean quiet);

  int localSize();

  Set<K> localKeySet();

  void unpinAll();

  boolean isPinned(K key);

  void setPinned(K key, boolean pinned);

  boolean containsLocalKey(Object key);

  void clearWithMetaData(MetaData metaData);

  V checkAndGetNonExpiredValue(K key, Object value, GetType getType, boolean quiet);

  void clearLocalCache();

  long localOnHeapSizeInBytes();

  long localOffHeapSizeInBytes();

  int localOnHeapSize();

  int localOffHeapSize();

  boolean containsKeyLocalOnHeap(Object key);

  boolean containsKeyLocalOffHeap(Object key);

  void unlockedPutNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                       int customMaxTTLSeconds, MetaData metaData);

  void unlockedRemoveNoReturnWithMetaData(Object key, MetaData metaData);

  public void unlockedClearWithMetaData(MetaData metaData);

  boolean isCompressionEnabled();

  boolean isCopyOnReadEnabled();

  void disposeLocally();

  ToolkitReadWriteLock createLockForKey(K key);
}
