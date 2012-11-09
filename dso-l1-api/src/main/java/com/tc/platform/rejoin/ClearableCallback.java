/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

public interface ClearableCallback {

  public void clearTimers();

  public void clearInternalDS();

  public void initTimers();

  public void cleanup();
}
