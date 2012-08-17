/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;

import java.util.Map;

public interface ServerMapLocalStoreListener<K, V> {

  public void notifyElementEvicted(K key, V value);

  public void notifyElementsEvicted(Map<K, V> evictedElements);

  public void notifyElementExpired(K key, V value);

}
