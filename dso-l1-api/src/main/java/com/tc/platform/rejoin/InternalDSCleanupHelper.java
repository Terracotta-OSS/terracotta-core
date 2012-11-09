/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

public abstract class InternalDSCleanupHelper extends CleanupHelper {

  @Override
  public abstract void clearInternalDS();

  @Override
  public void clearTimers() {
    //
  }

  @Override
  public void initTimers() {
    //
  }

}
