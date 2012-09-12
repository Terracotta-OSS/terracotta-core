/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;

public class PinnedEntryFaultContext implements EventContext {

  private static final TCLogger          LOGGER = TCLogging.getLogger(PinnedEntryFaultContext.class);
  private final Object                   key;
  private final boolean                  eventual;
  private final PinnedEntryFaultCallback callback;

  public PinnedEntryFaultContext(Object key, boolean eventual, PinnedEntryFaultCallback callback) {
    this.key = key;
    this.eventual = eventual;
    this.callback = callback;
  }

  public void prefetch() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Going to prefetch entry : " + key);
    }
    if (eventual) {
      callback.unlockedGet(key);
    } else {
      callback.get(key);
    }
  }
}
