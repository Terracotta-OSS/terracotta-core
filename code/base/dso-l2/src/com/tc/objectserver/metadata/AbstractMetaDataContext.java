/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

/**
 * 
 */
public abstract class AbstractMetaDataContext implements EventContext {

  private final ServerTransactionID txnID;

  public AbstractMetaDataContext(ServerTransactionID txnID) {
    this.txnID = txnID;
  }

  public NodeID getSourceID() {
    return txnID.getSourceID();
  }

  public ServerTransactionID getServerTransactionID() {
    return txnID;
  }

  public TransactionID getClientTransactionID() {
    return txnID.getClientTransactionID();
  }
}
