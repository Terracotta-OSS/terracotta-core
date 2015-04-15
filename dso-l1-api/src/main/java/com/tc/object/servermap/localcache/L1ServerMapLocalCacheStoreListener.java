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
package com.tc.object.servermap.localcache;

import java.util.Map;

/**
 * This interface would be called when an eviction happens in the L1ServerMapLocalStore.<br>
 * Currently this interface should be called only when:<br>
 * 1) capacity eviction happens<br>
 * 2) evict (count) gets called on L1ServerMapLocalStore<br>
 */
public interface L1ServerMapLocalCacheStoreListener<K, V> {

  /**
   * When a key gets evicted.
   */
  public void notifyElementEvicted(K key, V value);

  /**
   * When a set if keys get evicted.
   */
  public void notifyElementsEvicted(Map<K, V> evictedElements);

  /**
   * When a key is expired
   */
  public void notifyElementExpired(K key, V value);

  /**
   * Called whenever a store is diposed of
   */
  public void notifyDisposed(L1ServerMapLocalCacheStore store);
}
