/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;

import java.util.Collection;

public interface ClientGlobalTransactionManager extends GlobalTransactionManager {
  public void setLowWatermark(GlobalTransactionID lowWatermark);

  public void flush(LockID lockID);

  public void unpause();

  public void pause();
  
  public void starting();

  public void resendOutstanding();

  public Collection getTransactionSequenceIDs();
  
  public Collection getResentTransactionIDs();

  public boolean startApply(ChannelID committerID, TransactionID transactionID, GlobalTransactionID globalTransactionID);

  /**
   * Returns the number of transactions currently being accounted for.
   */
  public int size();

  public void resendOutstandingAndUnpause();

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback);
}
