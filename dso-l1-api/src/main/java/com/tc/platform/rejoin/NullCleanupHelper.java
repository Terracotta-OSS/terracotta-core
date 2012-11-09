/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

public class NullCleanupHelper extends CleanupHelper {

  @Override
  public void clearInternalDS() {
    // no-op
  }

  @Override
  public void clearTimers() {
    // no-op
  }

  @Override
  public void initTimers() {
    // no-op
  }

}
