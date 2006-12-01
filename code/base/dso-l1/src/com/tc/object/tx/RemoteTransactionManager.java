/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;

import java.util.Collection;

/**
 * Client representation of the server transaction manager
 *
 * @author steve
 */
public interface RemoteTransactionManager {

  public void pause();

  public void starting();

  public void unpause();

  public void stop();

  /**
   * Blocks until all of the transactions within the given lock have been fully acked.
   */
  public void flush(LockID lockID);

  public void commit(ClientTransaction transaction);

  public void receivedAcknowledgement(SessionID sessionID, TransactionID txID);

  public void receivedBatchAcknowledgement(TxnBatchID batchID);

  public void resendOutstanding();

  public Collection getTransactionSequenceIDs();

  public Collection getResentTransactionIDs();

  public void resendOutstandingAndUnpause();

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);
}