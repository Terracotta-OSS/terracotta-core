/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.TCObjectServerMap;
import com.tc.object.cache.CachedItem;

public class CachedItemExpiredContext implements EventContext {

  private final TCObjectServerMap serverMap;
  private final CachedItem        expired;

  public CachedItemExpiredContext(final TCObjectServerMap serverMap, final CachedItem expired) {
    this.serverMap = serverMap;
    this.expired = expired;
  }

  public TCObjectServerMap getTCObjectServerMap() {
    return this.serverMap;
  }

  public CachedItem getExpiredCachedItem() {
    return this.expired;
  }

}
