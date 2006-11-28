/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.util.SequenceID;

public interface GlobalTransaction {
  public GlobalTransactionID getGlobalTransactionID();
  public SequenceID getSequenceID();
}
