/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

public class CachedItemExpiredContext implements EventContext {

  private final ServerMapLocalCache          serverMapLocalCache;
  private final Object                       key;
  private final AbstractLocalCacheStoreValue value;

  public CachedItemExpiredContext(final ServerMapLocalCache serverMapLocalCache, final Object key,
                                  final AbstractLocalCacheStoreValue value) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.key = key;
    this.value = value;
  }

  public ServerMapLocalCache getServerMapLocalCache() {
    return this.serverMapLocalCache;
  }

  public Object getKey() {
    return this.key;
  }

  public AbstractLocalCacheStoreValue getValue() {
    return this.value;
  }

}
