/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

public class CommitTransactionChangeHandler extends AbstractEventHandler {

  private ServerTransactionManager             transactionManager;
  private final PersistenceTransactionProvider ptxp;
  private TransactionalObjectManager           txnObjectManager;

  public CommitTransactionChangeHandler(PersistenceTransactionProvider ptxp) {
    this.ptxp = ptxp;
  }

  public void handleEvent(EventContext context) {
    CommitTransactionContext ctc = (CommitTransactionContext) context;
    txnObjectManager.commitTransactionsComplete(ctc);
    if (ctc.isInitialized()) {
      transactionManager.commit(ptxp, ctc.getObjects(), ctc.getNewRoots(), ctc.getAppliedServerTransactionIDs(), ctc
          .getCompletedTransactionIDs());
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.txnObjectManager = scc.getTransactionalObjectManager();
  }

}
