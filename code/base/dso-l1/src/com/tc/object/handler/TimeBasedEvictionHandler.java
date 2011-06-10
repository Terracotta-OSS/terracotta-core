/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.cache.CachedItem;
import com.tc.object.context.CachedItemExpiredContext;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.ServerLockLevel;

public class TimeBasedEvictionHandler extends AbstractEventHandler {

  private ClientLockManager lockManager;

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
        if (ci.getID() instanceof LockID) {
          // recall the locks in-line to save memory and also to keep the local cache count in check
          this.lockManager.recall((LockID) ci.getID(), ServerLockLevel.WRITE, -1);
        }
      }
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }
}
