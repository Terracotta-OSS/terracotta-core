/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

public interface ServerTransactionListener {
  
  public void incomingTransactions(NodeID source, Set serverTxnIDs);
  
  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated);
  
  public void transactionCompleted(ServerTransactionID stxID);

  public void addResentServerTransactionIDs(Collection stxIDs);

  public void clearAllTransactionsFor(NodeID deadNode);

  public void transactionManagerStarted(Set cids);
  
}
