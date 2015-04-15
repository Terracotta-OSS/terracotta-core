/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.exception.ImplementMe;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eugene Shelestovich
 */
public class MockToolkitStore<K, V> extends ConcurrentHashMap<K, V> implements ToolkitStore<K, V>, ToolkitCacheInternal<K, V> {

  private final String name;

  public MockToolkitStore() {
    this("mockToolkitStore");
  }

  public MockToolkitStore(final String name) {
    this.name = name;
  }

  @Override
  public void removeNoReturn(final Object key) {
    super.remove(key);
  }

  @Override
  public void putNoReturn(final K key, final V value) {
    super.put(key, value);
  }

  @Override
  public void unlockedPutNoReturn(final K k, final V v, final int createTime, final int customTTI, final int customTTL) {
    super.put(k, v);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime, final int customTTI, final int customTTL) {
    super.put(k, v);
  }

  @Override
  public void unlockedRemoveNoReturn(final Object k) {
    super.remove(k);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    super.remove(key);
  }

  @Override
  public V unlockedGet(final Object k, final boolean quiet) {
    return super.get(k);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<K, V> getAll(final Collection<? extends K> keys) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Configuration getConfiguration() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void setConfigField(final String name, final Serializable value) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(final K key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V getQuiet(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Map<K, V> getAllQuiet(final Collection<K> keys) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putNoReturn(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds, final int maxTTLSeconds) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V putIfAbsent(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds, final int maxTTLSeconds) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void addListener(final ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void removeListener(final ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void setAttributeExtractor(final ToolkitAttributeExtractor<K, V> attrExtractor) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean isDestroyed() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void destroy() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(final Set portableKeys) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Map<K, V> unlockedGetAll(final Collection<K> keys, final boolean quiet) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void clearLocalCache() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V unsafeLocalGet(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public int localSize() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Set<K> localKeySet() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public long localOnHeapSizeInBytes() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public long localOffHeapSizeInBytes() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public int localOnHeapSize() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public int localOffHeapSize() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean containsKeyLocalOnHeap(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean containsKeyLocalOffHeap(final Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public V put(final K key, final V value, final int createTimeInSecs, final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void putIfAbsentVersioned(K key, V value, long version, int createTimeInSecs, int customMaxTTISeconds,
                                        int customMaxTTLSeconds) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void disposeLocally() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void removeAll(final Set<K> keys) {
    throw new UnsupportedOperationException("Implement me!");
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
  public void unregisterVersionUpdateListener(final VersionUpdateListener listener) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public Set<K> keySetForSegment(final int segmentIndex) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public VersionedValue<V> getVersionedValue(Object key) {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean isBulkLoadEnabled() {
    throw new UnsupportedOperationException("Implement me!");

  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) {
    throw new UnsupportedOperationException("Implement me!");

  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    throw new UnsupportedOperationException("Implement me!");

  }

  @Override
  public void clearVersioned() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public void quickClear() {
    throw new UnsupportedOperationException("Implement me!");

  }

  @Override
  public int quickSize() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public boolean remove(Object key, Object value, ToolkitValueComparator<V> comparator) {
    throw new ImplementMe();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue, ToolkitValueComparator<V> comparator) {
    throw new ImplementMe();
  }

  @Override
  public Map<K, VersionedValue<V>> getAllVersioned(final Collection<K> keys) {
    throw new UnsupportedOperationException("Implement me!");
  }
}
