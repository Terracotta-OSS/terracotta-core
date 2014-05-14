/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;

import java.util.List;

/**
 * Transaction context
 */
public interface TransactionContext {

  /**
   * Returns the transaction type that corresponds to the lock type that
   * initiated this transaction.
   * 
   * @return Type of transaction based on the lock
   * @see #getEffectiveType()
   */
  public TxnType getLockType();

  /**
   * Returns the effective transaction type.
   *
   * Note that this can be different from the type that was requested by the
   * lock when that lock is nested within another lock. For instance, if a
   * write lock is already active and a nested read lock is obtained, the 
   * operations are effectively guarded against writes and not only reads.
   * 
   * @return Type of transaction based on the context
   * @see #getLockType()
   */
  public TxnType getEffectiveType();
  
  /**
   * @return First lock identifier in the transaction
   */
  public LockID getLockID();

  /**
   * @return All lock identifiers that have been involved in the transaction
   */
  public List getAllLockIDs();

  /**
   * Remove a lock previously involved in the transaction
   * 
   * @param id Identifier
   */
  public void removeLock(LockID id);

}
