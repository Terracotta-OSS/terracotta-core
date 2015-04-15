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
package com.tc.object.gtx;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.NodeID;
import com.tc.object.ClearableCallback;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;

public interface ClientGlobalTransactionManager extends GlobalTransactionManager, ClearableCallback {
  public void setLowWatermark(GlobalTransactionID lowWatermark, NodeID nodeID);

  public void flush(LockID lockID) throws AbortedOperationException;

  public boolean startApply(NodeID clientID, TransactionID transactionID, GlobalTransactionID globalTransactionID,
                            NodeID remoteGroupID);

  /**
   * Returns the number of transactions currently being accounted for.
   */
  public int size();

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback);

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) throws AbortedOperationException;
}
