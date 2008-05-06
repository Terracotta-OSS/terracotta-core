/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.msg.MessageRecyclerImpl;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.Set;

public class CommitTransactionMessageRecycler extends MessageRecyclerImpl implements ServerTransactionListener {

  public CommitTransactionMessageRecycler(ServerTransactionManager transactionManager) {
    transactionManager.addTransactionListener(this);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    recycle(stxID);
  }

  public void transactionApplied(ServerTransactionID stxID) {
    return;
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    return;
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    return;
  }

  public void clearAllTransactionsFor(NodeID deadNode) {
    return;
  }

  public void transactionManagerStarted(Set cids) {
    return;
  }

}
