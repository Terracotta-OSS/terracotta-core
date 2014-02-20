/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
