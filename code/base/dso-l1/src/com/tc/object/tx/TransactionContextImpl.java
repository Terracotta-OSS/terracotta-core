/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockID;

import java.util.ArrayList;
import java.util.List;

public class TransactionContextImpl implements TransactionContext {
  private final TxnType type;
  private final LockID  lockID;
  private final List    lockIDs;

  public TransactionContextImpl(LockID lockID, TxnType type) {
    this.type = type;
    this.lockID = lockID;
    this.lockIDs = new ArrayList();
    lockIDs.add(lockID);
  }
  
  // assume lockIDs contains lockID
  public TransactionContextImpl(LockID lockID, TxnType type, List lockIDs) {
    this.type = type;
    this.lockID = lockID;
    this.lockIDs = lockIDs;    
  }
  
  public TxnType getType() {
    return type;
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