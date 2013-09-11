/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.bulkload;

import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.tc.abortable.AbortedOperationException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.abortable.ToolkitAbortableOperationException;
import com.terracotta.toolkit.collections.map.AggregateServerMap;
import com.terracotta.toolkit.collections.map.ToolkitCacheImplInterface;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BulkLoadToolkitCache<K, V> implements ToolkitCacheImplInterface<K, V> {
  private static final TCLogger          LOGGER                                 = TCLogging
                                                                                    .getLogger(BulkLoadToolkitCache.class);
  private final AggregateServerMap<K, V> toolkitCache;
  private final BulkLoadEnabledNodesSet  bulkLoadEnabledNodesSet;
  private boolean                        localCacheEnabledBeforeBulkloadEnabled = false;
  private final PlatformService          platformService;
  private final String                   name;
  private final BulkLoadShutdownHook     bulkLoadShutdownHook;
  private final boolean                  loggingEnabled;
  private final LocalBufferedMap<K, V>   localBufferedMap;
  private final BulkLoadConstants        bulkLoadConstants;

  public BulkLoadToolkitCache(PlatformService platformService, String name,
                              AggregateServerMap<K, V> aggregateServerMap, ToolkitInternal toolkit) {
    this.name = name;
    this.toolkitCache = aggregateServerMap;
    this.platformService = platformService;
    this.bulkLoadConstants = new BulkLoadConstants(platformService.getTCProperties());
    this.bulkLoadEnabledNodesSet = new BulkLoadEnabledNodesSet(platformService, name, toolkit, bulkLoadConstants);
    this.bulkLoadShutdownHook = new BulkLoadShutdownHook(platformService);
    this.loggingEnabled = bulkLoadConstants.isLoggingEnabled();

    this.localBufferedMap = new LocalBufferedMap(name, aggregateServerMap, toolkit, bulkLoadConstants);
  }

  public void debug(String msg) {
    LOGGER.debug("['" + name + "'] " + msg);
  }

  private void exitBulkLoadMode() {
    if (loggingEnabled) {
      debug("Turning off bulk-load");
    }
    try {
      platformService.waitForAllCurrentTransactionsToComplete();
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }
    // clear local cache
    toolkitCache.clearLocalCache();
    // flush and stop local buffering
    localBufferedMap.flushAndStopBuffering();
    // enable local cache
    setLocalCacheEnabled(localCacheEnabledBeforeBulkloadEnabled);

    // remove current node from list of bulk-loading nodes
    bulkLoadEnabledNodesSet.removeCurrentNode();

    bulkLoadShutdownHook.unregisterCache(this);
    // wait until all txns finished
    try {
      platformService.waitForAllCurrentTransactionsToComplete();
    } catch (AbortedOperationException e) {
      throw new ToolkitAbortableOperationException(e);
    }

  }

  private void setLocalCacheEnabled(boolean enabled) {
    new ToolkitCacheConfigBuilder().localCacheEnabled(enabled).apply((ToolkitCache) toolkitCache);
  }

  @Override
  public V getQuiet(Object key) {
    return doGet(key, true);
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    localBufferedMap.put(key, value, LocalBufferedMap.NO_VERSION, (int) createTimeInSecs, customMaxTTISeconds,
                         customMaxTTLSeconds);
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    V value = localBufferedMap.get(key);
    if (value == null) {
      value = toolkitCache.unsafeLocalGet(key);
    }
    return value;
  }

  @Override
  public void putNoReturn(K key, V value) {
    putNoReturn(key, value, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public int localSize() {
    return localBufferedMap.getSize() + toolkitCache.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return new UnmodifiableMultiSetWrapper<K>(localBufferedMap.getKeys(), toolkitCache.localKeySet());

  }

  @Override
  public boolean containsLocalKey(Object key) {
    return localBufferedMap.containsKey(key) || toolkitCache.containsLocalKey(key);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, doGet(key, false));
    }
    return rv;
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, doGet(key, true));
    }
    return rv;
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    toolkitCache.addListener(listener);
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    toolkitCache.removeListener(listener);
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return toolkitCache.createLockForKey(key);
  }

  @Override
  public Configuration getConfiguration() {
    return toolkitCache.getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    if (name.equals(ToolkitConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME)) localCacheEnabledBeforeBulkloadEnabled = (Boolean) value;
    else toolkitCache.setConfigField(name, value);
  }

  @Override
  public boolean containsKey(Object keyObj) {
    K key = (K) keyObj;
    return localBufferedMap.containsKey(key) || toolkitCache.containsKey(key);
  }

  @Override
  public boolean containsValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new UnmodifiableMultiSetWrapper<Map.Entry<K, V>>(localBufferedMap.entrySet(), toolkitCache.entrySet());
  }

  @Override
  public V get(Object obj) {
    return doGet(obj, false);
  }

  public V doGet(Object obj, boolean quiet) {
    K key = (K) obj;
    if (localBufferedMap.isKeyBeingRemoved(obj)) { return null; }

    V value = localBufferedMap.get(key);
    if (value == null) {
      value = toolkitCache.unlockedGet(obj, quiet);
    }
    return value;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<K> keySet() {
    return new UnmodifiableMultiSetWrapper<K>(localBufferedMap.getKeys(), toolkitCache.keySet());
  }

  private int now() {
    return (int) (System.currentTimeMillis() / 1000);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    for (K key : keys) {
      remove(key);
    }
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    localBufferedMap.remove((K) key, version);
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    toolkitCache.registerVersionUpdateListener(listener);
  }

  @Override
  public int size() {
    return localBufferedMap.getSize() + toolkitCache.size();
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return toolkitCache.getName();
  }

  @Override
  public boolean isDestroyed() {
    return toolkitCache.isDestroyed();
  }

  @Override
  public void destroy() {
    bulkLoadEnabledNodesSet.disposeLocally();
    toolkitCache.destroy();
  }

  @Override
  public void disposeLocally() {
    setNodeBulkLoadEnabled(false);
    bulkLoadEnabledNodesSet.disposeLocally();
    toolkitCache.disposeLocally();
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
  public void clear() {
    localBufferedMap.clear();
    toolkitCache.clear();
  }

  @Override
  public void removeNoReturn(Object key) {
    remove(key);
  }

  @Override
  public V remove(Object key) {
    V rv = localBufferedMap.remove((K) key, LocalBufferedMap.NO_VERSION);
    if (rv == null) {
      rv = toolkitCache.get(key);
    }
    return rv;
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return toolkitCache.createQueryBuilder();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return toolkitCache.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return toolkitCache.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return localBufferedMap.getSize() + toolkitCache.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return toolkitCache.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return localBufferedMap.containsKey(key) || toolkitCache.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return toolkitCache.containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value) {
    return put(key, value, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    V rv = toolkitCache.unlockedGet(key, true);
    putNoReturn(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    return rv;
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    putVersioned(key, value, version, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS,
                 ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {

    localBufferedMap.put(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);

  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    return toolkitCache.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    localBufferedMap.put(k, v, LocalBufferedMap.NO_VERSION, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    localBufferedMap.put(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    localBufferedMap.remove((K) k, LocalBufferedMap.NO_VERSION);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    localBufferedMap.remove((K) key, version);
  }

  @Override
  public void clearLocalCache() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return doGet(k, quiet);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor<K, V> extractor) {
    toolkitCache.setAttributeExtractor(extractor);
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return quiet ? getAllQuiet(keys) : getAll(keys);
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return bulkLoadEnabledNodesSet.isBulkLoadEnabledInCluster();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return bulkLoadEnabledNodesSet.isBulkLoadEnabledInNode();
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    if (enabledBulkLoad) {
      // turning on bulk-load
      if (!bulkLoadEnabledNodesSet.isBulkLoadEnabledInNode()) {
        enterBulkLoadMode();
      } else {
        if (loggingEnabled) {
          LOGGER.warn("Trying to enable bulk-load mode when already bulk-loading.");
        }
      }
    } else {
      // turning off bulk-load
      if (bulkLoadEnabledNodesSet.isBulkLoadEnabledInNode()) {
        exitBulkLoadMode();
      } else {
        if (loggingEnabled) {
          LOGGER.warn("Trying to disable bulk-load mode when not bulk-loading.");
        }
      }
    }
  }

  private void enterBulkLoadMode() {
    if (loggingEnabled) {
      debug("Enabling bulk-load");
    }

    localCacheEnabledBeforeBulkloadEnabled = toolkitCache.getConfiguration()
        .getBoolean(ToolkitConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME);

    // disable local cache
    if (localCacheEnabledBeforeBulkloadEnabled) {
      setLocalCacheEnabled(false);
    }

    // add current node
    bulkLoadEnabledNodesSet.addCurrentNode();
    bulkLoadShutdownHook.registerCache(this);

    localBufferedMap.startBuffering();

  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    bulkLoadEnabledNodesSet.waitUntilSetEmpty();
  }

  public void rejoinCleanUp() {
    localBufferedMap.clear();
  }

  public void rejoinCompleted() {
    bulkLoadEnabledNodesSet.addCurrentNodeInternal();
  }
}
