/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.List;

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
  public abstract List getAllLockIDs();

  /**
   * Remove a lock previously involved in the transaction
   * 
   * @param id Identifier
   */
  public abstract void removeLock(LockID id);

}