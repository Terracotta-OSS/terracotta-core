/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

public class NullCleanupHelper implements ClearableCallback {

  @Override
  public void cleanup() {
    // no-op
  }
}
