/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;

public class TestClientGlobalTransactionManager implements ClientGlobalTransactionManager {

  public final NoExceptionLinkedQueue flushCalls = new NoExceptionLinkedQueue();
  public Collection                   transactionSequenceIDs;

  @Override
  public void cleanup() {
    throw new ImplementMe();
  }

  @Override
  public void setLowWatermark(GlobalTransactionID lowWatermark, NodeID nodeID) {
    throw new ImplementMe();
  }

  @Override
  public void flush(LockID lockID, boolean noLocksLeftOnClient) {
    flushCalls.put(lockID);
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    throw new ImplementMe();
  }

  @Override
  public int size() {
    throw new ImplementMe();
  }

  @Override
  public boolean asyncFlush(LockID lockID, LockFlushCallback callback, boolean noLocksLeftOnClient) {
    return true;
  }

  @Override
  public boolean startApply(NodeID clientID, TransactionID transactionID, GlobalTransactionID globalTransactionID,
                            NodeID remoteGroupID) {
    throw new ImplementMe();
  }

  @Override
  public void waitForServerToReceiveTxnsForThisLock(LockID lock) {
    throw new ImplementMe();
  }
}
