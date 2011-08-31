/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;

import java.util.Set;

public class SyncWriteTransactionReceivedContext implements EventContext {
  private final long               batchID;
  private final ClientID           cid;
  private final Set<TransactionID> txIdSet;

  public SyncWriteTransactionReceivedContext(long batchID, ClientID cid, Set<TransactionID> set) {
    this.batchID = batchID;
    this.cid = cid;
    this.txIdSet = set;
  }

  public long getBatchID() {
    return batchID;
  }

  public ClientID getClientID() {
    return cid;
  }

  public Set<TransactionID> getSyncTransactions() {
    return txIdSet;
  }
}
