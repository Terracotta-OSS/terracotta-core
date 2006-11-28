/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.gtx.GlobalTransactionID;


public interface TransactionApplyResults {
  public GlobalTransactionID getGlobalTransactionID();
  public GlobalTransactionID getLowGlobalTransactionIDWatermark();
}
