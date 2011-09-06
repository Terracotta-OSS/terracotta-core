/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;

/**
 * Client Transaction Factory interface
 */
public interface ClientTransactionFactory {
  public ClientTransaction newNullInstance(LockID id, TxnType type);

  public ClientTransaction newInstance();

}