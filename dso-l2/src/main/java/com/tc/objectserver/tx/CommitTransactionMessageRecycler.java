/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.net.NodeID;
import com.tc.object.msg.MessageRecyclerImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

public class CommitTransactionMessageRecycler extends MessageRecyclerImpl implements ServerTransactionListener,
    PostInit {

  @Override
  public void initializeContext(ConfigurationContext context) {
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    ServerTransactionManager transactionManager = scc.getTransactionManager();
    transactionManager.addTransactionListener(this);
  }

  @Override
  public void transactionCompleted(ServerTransactionID stxID) {
    recycle(stxID);
  }

  @Override
  public void transactionApplied(ServerTransactionID stxID, ObjectIDSet newObjectsCreated) {
    return;
  }

  @Override
  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    return;
  }

  @Override
  public void addResentServerTransactionIDs(Collection stxIDs) {
    return;
  }

  @Override
  public void clearAllTransactionsFor(NodeID deadNode) {
    return;
  }

  @Override
  public void transactionManagerStarted(Set cids) {
    return;
  }

}
