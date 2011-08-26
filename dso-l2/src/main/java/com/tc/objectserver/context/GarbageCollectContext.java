/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.util.Util;

import java.util.concurrent.CountDownLatch;

public class GarbageCollectContext implements EventContext {

  private final CountDownLatch completionLatch = new CountDownLatch(1);
  private final GCType         type;
  private long                 delay;

  public GarbageCollectContext(final GCType type, final long delay) {
    this.type = type;
    this.delay = delay;
  }

  public GarbageCollectContext(final GCType type) {
    this(type, 0);
  }

  public void done() {
    completionLatch.countDown();
  }

  public void waitForCompletion() {
    boolean isInterrupted = false;
    while (completionLatch.getCount() > 0) {
      try {
        completionLatch.await();
      } catch (InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public GCType getType() {
    return type;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(final long delay) {
    this.delay = delay;
  }
}
