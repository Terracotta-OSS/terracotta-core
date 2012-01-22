/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

public interface GlobalTransactionManager {

  /**
   * Returns the least GlobalTransactionID that is still active.
   */
  public GlobalTransactionID getLowGlobalTransactionIDWatermark();
}
