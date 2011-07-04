/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

public class CommitTransactionChangeHandler extends AbstractEventHandler {

  private ServerTransactionManager             transactionManager;
  private final PersistenceTransactionProvider ptxp;
  private TransactionalObjectManager           txnObjectManager;

  public CommitTransactionChangeHandler(final PersistenceTransactionProvider ptxp) {
    this.ptxp = ptxp;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final CommitTransactionContext ctc = (CommitTransactionContext) context;
    this.txnObjectManager.commitTransactionsComplete(ctc);
    if (ctc.isInitialized()) {
      this.transactionManager.commit(this.ptxp, ctc.getObjects(), ctc.getNewRoots(), ctc
          .getAppliedServerTransactionIDs());
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.txnObjectManager = scc.getTransactionalObjectManager();
  }

}
