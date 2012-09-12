/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

public class PinnedEntryFaultHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(EventContext context) {
    PinnedEntryFaultContext prefetchContext = (PinnedEntryFaultContext) context;
    prefetchContext.prefetch();
  }

}
