/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;

public interface ServerMapLocalStoreFactory {

  /**
   * Return the {@link ServerMapLocalStore} for the associated config. Creates one, if not already created, using the
   * name.
   */
  <K, V> ServerMapLocalStore<K, V> getOrCreateServerMapLocalStore(ServerMapLocalStoreConfig config);
}
