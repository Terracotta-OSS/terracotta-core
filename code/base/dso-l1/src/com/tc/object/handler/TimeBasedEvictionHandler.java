/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.cache.CachedItem;
import com.tc.object.context.CachedItemExpiredContext;

public class TimeBasedEvictionHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(final EventContext context) {
    final CachedItemExpiredContext ev = (CachedItemExpiredContext) context;
    final TCObjectServerMap serverMapTC = ev.getTCObjectServerMap();
    final TCServerMap serverMap = (TCServerMap) serverMapTC.getPeerObject();
    if (serverMap != null) {
      final CachedItem ci = ev.getExpiredCachedItem();
      final Object value = ci.getValue();
      if (value != null) { // If null, its Already removed
        serverMap.evictExpired(ci.getKey(), value);
      }
    }
  }
}
