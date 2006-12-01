/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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