/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

public abstract class AbstractServerTransactionListener implements ServerTransactionListener {

  public void addResentServerTransactionIDs(Collection stxIDs) {
    // Override if you want
  }

  public void clearAllTransactionsFor(NodeID deadNode) {
    // Override if you want
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    // Override if you want
  }

  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
    // Override if you want
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    // Override if you want
  }

  public void transactionManagerStarted(Set cids) {
    // Override if you want
  }
}
