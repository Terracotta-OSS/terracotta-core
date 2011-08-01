/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class PeriodicGarbageCollectContext extends GarbageCollectContext {
  private static final TCLogger logger = TCLogging.getLogger(PeriodicGarbageCollectContext.class);
  private final long            interval;

  public PeriodicGarbageCollectContext(final GCType type, final long interval) {
    super(type, interval);
    this.interval = interval;
  }

  @Override
  public void done() {
    // do nothing
  }

  @Override
  public void waitForCompletion() {
    logger.warn("Attempted to wait for completion on Periodic garbage collection.");
  }

  public long getInterval() {
    return interval;
  }

  public void reset() {
    setDelay(interval);
  }
}
