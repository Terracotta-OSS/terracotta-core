/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;

public interface ClientGlobalTransactionManager extends GlobalTransactionManager {
  public void setLowWatermark(GlobalTransactionID lowWatermark, NodeID nodeID);

  public void flush(LockID lockID, boolean noLocksLeftOnClient);

  public boolean startApply(NodeID clientID, TransactionID transactionID, GlobalTransactionID globalTransactionID,
                            NodeID remoteGroupID);

  /**
   * Returns the number of transactions currently being accounted for.
   */
  public int size();

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback, boolean noLocksLeftOnClient);

  public void waitForServerToReceiveTxnsForThisLock(LockID lock);
}
