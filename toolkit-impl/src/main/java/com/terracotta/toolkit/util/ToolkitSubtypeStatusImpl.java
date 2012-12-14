/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import com.tc.util.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ToolkitSubtypeStatusImpl implements ToolkitSubtypeStatus {
  private final AtomicInteger rejoinCount = new AtomicInteger(0);
  private final AtomicBoolean isDestroyed = new AtomicBoolean(false);

  public int getRejoinCount() {
    return rejoinCount.get();
  }

  public void increaseRejoinCount() {
    this.rejoinCount.incrementAndGet();
  }

  public void setDestroyed() {
    Assert.assertFalse(isDestroyed.get());
    this.isDestroyed.set(true);
  }

  @Override
  public int getCurrentRejoinCount() {
    return rejoinCount.get();
  }

  @Override
  public boolean isDestroyed() {
    return isDestroyed.get();
  }

}
