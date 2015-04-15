/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
  
  @Override
  public TxnType getLockType() {
    return lockTxType;
  }

  @Override
  public TxnType getEffectiveType() {
    return effectiveTxType;
  }
  
  @Override
  public LockID getLockID() {
    return lockID;
  }

  @Override
  public List getAllLockIDs() {
    return lockIDs;
  }

  @Override
  public void removeLock(LockID id) {
    lockIDs.remove(id);
  }
}