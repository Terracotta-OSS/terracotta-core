/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.locks.LockFlushCallback;
import com.tc.object.locks.LockID;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrintable;

import java.util.Set;

/**
 * Client representation of the server transaction manager
 */
public interface RemoteTransactionManager extends ClientHandshakeCallback, PrettyPrintable {

  public void stop();

  /**
   * Blocks until all of the transactions within the given lock has been fully ACKed.
   */
  public void flush(LockID lockID);

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback);

  public void commit(ClientTransaction transaction);

  public TransactionBuffer receivedAcknowledgement(SessionID sessionID, TransactionID txID, NodeID nodeID);

  public void receivedBatchAcknowledgement(TxnBatchID batchID, NodeID nodeID);

  public void stopProcessing();

  public void waitForAllCurrentTransactionsToComplete();

  public void waitForServerToReceiveTxnsForThisLock(LockID lock);

  public void batchReceived(TxnBatchID batchId, Set<TransactionID> set, NodeID nid);
}
