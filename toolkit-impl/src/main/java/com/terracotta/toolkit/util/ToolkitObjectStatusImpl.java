/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import com.tc.util.Assert;

public class ToolkitObjectStatusImpl implements ToolkitObjectStatus {
  private volatile int     rejoinCount;
  private volatile boolean isDestroyed;
  private volatile boolean isRejoinInProgress;

  public int getRejoinCount() {
    return rejoinCount;
  }

  public void incrementRejoinCount() {
    this.rejoinCount++;
  }

  public void setDestroyed() {
    Assert.assertFalse(isDestroyed);
    this.isDestroyed = true;
  }

  public void setRejoinInProgress(boolean val) {
    isRejoinInProgress = val;
  }

  @Override
  public int getCurrentRejoinCount() {
    return rejoinCount;
  }

  @Override
  public boolean isDestroyed() {
    return isDestroyed;
  }

  @Override
  public boolean isRejoinInProgress() {
    return isRejoinInProgress;
  }

}
