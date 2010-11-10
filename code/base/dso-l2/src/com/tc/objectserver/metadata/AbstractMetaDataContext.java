/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.metadata;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.tx.TransactionID;

/**
 * 
 */
public abstract class AbstractMetaDataContext implements EventContext {
  
  private final TransactionID transactionID;
  private final NodeID id;
  
  public AbstractMetaDataContext(NodeID nodeID, TransactionID transactionID) {
    this.id = nodeID;
    this.transactionID = transactionID;
  }
  
  public TransactionID getTransactionID() {
    return transactionID;
  }
  
  public NodeID getSourceID() {
    return id;
  }

}
