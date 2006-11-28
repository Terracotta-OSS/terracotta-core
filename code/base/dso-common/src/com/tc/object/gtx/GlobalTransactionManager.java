/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.gtx;

public interface GlobalTransactionManager {

  /**
   * Returns the least GlobalTransactionID that is still active.
   */
  public GlobalTransactionID getLowGlobalTransactionIDWatermark();
}
