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

import com.tc.abortable.AbortedOperationException;
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
  public void flush(LockID lockID) throws AbortedOperationException;

  public boolean asyncFlush(LockID lockID, LockFlushCallback callback);

  public void commit(ClientTransaction transaction) throws AbortedOperationException;

  public TransactionBuffer receivedAcknowledgement(SessionID sessionID, TransactionID txID, NodeID nodeID);

  public void receivedBatchAcknowledgement(TxnBatchID batchID, NodeID nodeID);
  
  public void throttleProcessing(boolean processing);

  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException;

  public void waitForServerToReceiveTxnsForThisLock(LockID lock) throws AbortedOperationException;

  public void batchReceived(TxnBatchID batchId, Set<TransactionID> set, NodeID nid);

  /**
   * This will mark state as REJOIN_IN_PROGRESS and throw threads out which are waiting in TransactionSequencer and
   * LockAccounting
   */
  public void preCleanup();

  public void requestImmediateShutdown();

}