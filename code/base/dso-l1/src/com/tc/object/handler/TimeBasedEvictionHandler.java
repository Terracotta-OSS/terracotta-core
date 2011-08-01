/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.context.CachedItemExpiredContext;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

public class TimeBasedEvictionHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(final EventContext context) {
    final CachedItemExpiredContext ev = (CachedItemExpiredContext) context;

    final ServerMapLocalCache serverMapLocalCache = ev.getServerMapLocalCache();
    serverMapLocalCache.evictExpired(ev.getKey(), ev.getValue());
  }
}
