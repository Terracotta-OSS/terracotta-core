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
package com.terracotta.toolkit.collections.servermap.api;

import java.util.List;

public interface ServerMapLocalStore<K, V> {

  public V put(K key, V value) throws ServerMapLocalStoreFullException;

  public V get(K key);

  public V remove(K key);

  public V remove(K key, V value);

  public void clear();

  public void cleanLocalState();

  public boolean addListener(ServerMapLocalStoreListener<K, V> listener);

  public boolean removeListener(ServerMapLocalStoreListener<K, V> listener);

  public List<K> getKeys();

  public int getMaxEntriesLocalHeap();

  public void setMaxEntriesLocalHeap(int newMaxEntriesLocalHeap);

  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap);

  public long getMaxBytesLocalHeap();

  public int getOffHeapSize();

  public int getOnHeapSize();

  public int getSize();

  public long getOnHeapSizeInBytes();

  public long getOffHeapSizeInBytes();

  public void dispose();

  public boolean containsKeyOnHeap(K key);

  public boolean containsKeyOffHeap(K key);

  public void recalculateSize(K key);

  public boolean isPinned();
}
