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

  @Override
  public void addResentServerTransactionIDs(final Collection stxIDs) {
    // Override if you want
  }

  @Override
  public void clearAllTransactionsFor(final NodeID deadNode) {
    // Override if you want
  }

  @Override
  public void incomingTransactions(final NodeID source, final Set serverTxnIDs) {
    // Override if you want
  }

  @Override
  public void transactionApplied(final ServerTransactionID stxID, final ObjectIDSet newObjectsCreated) {
    // Override if you want
  }

  @Override
  public void transactionCompleted(final ServerTransactionID stxID) {
    // Override if you want
  }

  @Override
  public void transactionManagerStarted(final Set cids) {
    // Override if you want
  }
}
