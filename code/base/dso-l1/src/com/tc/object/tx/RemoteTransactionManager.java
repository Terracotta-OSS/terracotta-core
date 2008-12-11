/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;

/**
 * Client representation of the server transaction manager
 */
public interface RemoteTransactionManager {

  public void stop();

  /**
   * Blocks until all of the transactions within the given lock has been fully ACKed.
   */
  public void flush(LockID lockID);

  public void commit(ClientTransaction transaction);

  public void receivedAcknowledgement(SessionID sessionID, TransactionID txID, NodeID nodeID);

  public void receivedBatchAcknowledgement(TxnBatchID batchID, NodeID nodeID);

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);

  public void stopProcessing();
}