/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.TCObjectServerMap;

public class CachedItemEvictionContext implements EventContext {

  private final TCObjectServerMap serverMap;

  public CachedItemEvictionContext(final TCObjectServerMap serverMap) {
    this.serverMap = serverMap;
  }

  public TCObjectServerMap getTCObjectServerMap() {
    return this.serverMap;
  }

}
