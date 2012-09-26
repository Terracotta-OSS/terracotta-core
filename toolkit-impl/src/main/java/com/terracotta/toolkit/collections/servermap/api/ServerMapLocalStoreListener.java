/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;


public interface ServerMapLocalStoreListener<K, V> {
  public void notifyElementEvicted(K key, V value);
}
