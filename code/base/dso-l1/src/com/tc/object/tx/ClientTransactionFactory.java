/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

/**
 * @author steve
 */
public interface ClientTransactionFactory {
  public ClientTransaction newNullInstance(LockID id, TxnType type);

  public ClientTransaction newInstance();

}