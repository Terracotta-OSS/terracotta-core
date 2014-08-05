/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util;

import com.tc.platform.PlatformService;
import com.tc.util.Assert;

public class ToolkitObjectStatusImpl implements ToolkitObjectStatus {
  private volatile boolean      isDestroyed;
  private final PlatformService service;

  public ToolkitObjectStatusImpl(PlatformService platformService) {
    service = platformService;
  }

  public void setDestroyed() {
    Assert.assertFalse(isDestroyed);
    this.isDestroyed = true;
  }

  @Override
  public int getCurrentRejoinCount() {
    return service.getRejoinCount();
  }

  @Override
  public boolean isDestroyed() {
    return isDestroyed;
  }

  @Override
  public boolean isRejoinInProgress() {
    return service.isRejoinInProgress();
  }

}
