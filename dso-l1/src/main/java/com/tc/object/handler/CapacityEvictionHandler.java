/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.context.CachedItemEvictionContext;

public class CapacityEvictionHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(final EventContext context) {
    final CachedItemEvictionContext ev = (CachedItemEvictionContext) context;
    ev.getTCObjectServerMap().doCapacityEviction();
  }

}
