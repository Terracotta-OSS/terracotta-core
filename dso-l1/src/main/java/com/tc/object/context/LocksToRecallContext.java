/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.object.locks.LockID;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class LocksToRecallContext implements EventContext {

  private final Set<LockID>    toRecall;
  private final CountDownLatch latch;

  public LocksToRecallContext(final Set<LockID> toRecall) {
    this.toRecall = toRecall;
    this.latch = new CountDownLatch(1);
  }

  public Set<LockID> getLocksToRecall() {
    return this.toRecall;
  }

  public void waitUntilRecallComplete() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void recallComplete() {
    latch.countDown();
  }

}
