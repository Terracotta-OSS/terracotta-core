/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.locks.LockID;

import java.util.ArrayList;
import java.util.List;

public class TransactionContextImpl implements TransactionContext {
  private final TxnType lockTxType;
  private final TxnType effectiveTxType;
  private final LockID  lockID;
  private final List    lockIDs;

  public TransactionContextImpl(final LockID lockID, final TxnType lockTxType, final TxnType effectiveTxType) {
    this.lockTxType = lockTxType;
    this.effectiveTxType = effectiveTxType;
    this.lockID = lockID;
    this.lockIDs = new ArrayList();
    lockIDs.add(lockID);
  }
  
  // assume lockIDs contains lockID
  public TransactionContextImpl(final LockID lockID, final TxnType lockTxType, final TxnType effectiveTxType, final List lockIDs) {
    this.lockTxType = lockTxType;
    this.effectiveTxType = effectiveTxType;
    this.lockID = lockID;
    this.lockIDs = lockIDs;    
  }
  
  public TxnType getLockType() {
    return lockTxType;
  }

  public TxnType getEffectiveType() {
    return effectiveTxType;
  }
  
  public LockID getLockID() {
    return lockID;
  }

  public List getAllLockIDs() {
    return lockIDs;
  }

  public void removeLock(LockID id) {
    lockIDs.remove(id);
  }
}