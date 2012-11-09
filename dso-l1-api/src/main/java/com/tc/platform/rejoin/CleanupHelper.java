/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

public abstract class CleanupHelper {

  public abstract void clearTimers();

  public abstract void clearInternalDS();

  public abstract void initTimers();

  public void cleanup() {
    clearTimers();
    clearInternalDS();
    initTimers();
  }

  public static void cleanup(ClearableCallback clearable) {
    clearable.clearTimers();
    clearable.clearInternalDS();
    clearable.initTimers();
  }
}
