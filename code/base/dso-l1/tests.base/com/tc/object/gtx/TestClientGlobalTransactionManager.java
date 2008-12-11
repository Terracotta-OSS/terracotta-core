/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;

public class TestClientGlobalTransactionManager implements ClientGlobalTransactionManager {

  public final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
  public Collection                   transactionSequenceIDs;

  public void setLowWatermark(GlobalTransactionID lowWatermark, NodeID nodeID) {
    throw new ImplementMe();
  }

  public void flush(LockID lockID) {
    flushCalls.put(lockID);
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    throw new ImplementMe();
  }

  public int size() {
    throw new ImplementMe();
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return true;
  }

  public boolean startApply(NodeID clientID, TransactionID transactionID, GlobalTransactionID globalTransactionID,
                            NodeID remoteGroupID) {
    throw new ImplementMe();
  }
}
