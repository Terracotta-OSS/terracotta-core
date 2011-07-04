/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

public interface GlobalTransactionManager {

  /**
   * Returns the least GlobalTransactionID that is still active.
   */
  public GlobalTransactionID getLowGlobalTransactionIDWatermark();
}
