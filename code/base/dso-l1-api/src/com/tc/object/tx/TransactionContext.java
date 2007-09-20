/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

/**
 * Transaction context 
 */
public interface TransactionContext {

  /**
   * @return Type of transaction
   */
  public abstract TxnType getType();

  /**
   * @return First lock identifier in the transaction
   */
  public abstract LockID getLockID();

  /**
   * @return All lock identifiers that have been involved in the transaction
   */
  public abstract LockID[] getAllLockIDs();

  /**
   * Remove a lock previously involved in the transaction
   * @param id Identifier
   */
  public abstract void removeLock(LockID id);

}