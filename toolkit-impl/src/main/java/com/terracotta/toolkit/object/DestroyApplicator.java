/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object;

/**
 * @author Abhishek Sanoujam
 */
public interface DestroyApplicator {

  /**
   * Apply destroy
   */
  void applyDestroy();

  /**
   * Register a callback that will be invoked when a destroy broadcast is received
   */
  void setApplyDestroyCallback(DestroyApplicator applyDestroyCallback);
}
