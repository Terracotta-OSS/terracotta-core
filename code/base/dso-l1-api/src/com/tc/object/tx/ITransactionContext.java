/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

public interface ITransactionContext {

  public abstract TxnType getType();

  public abstract LockID getLockID();

  public abstract LockID[] getAllLockIDs();

  public abstract void removeLock(LockID id);

}