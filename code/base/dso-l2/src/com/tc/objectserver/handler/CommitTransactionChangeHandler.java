/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.objectserver.context.BatchedTransactionProcessingContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransactionManager;

public class CommitTransactionChangeHandler extends AbstractEventHandler {

  private ServerTransactionManager             transactionManager;
  private final ServerGlobalTransactionManager gtxm;
  private final PersistenceTransactionProvider ptxp;

  public CommitTransactionChangeHandler(  ServerGlobalTransactionManager gtxm, PersistenceTransactionProvider ptxp) {
    this.gtxm = gtxm;
    this.ptxp = ptxp;
  }

  public void handleEvent(EventContext context) {
    BatchedTransactionProcessingContext btpc = (BatchedTransactionProcessingContext) context;
    PersistenceTransaction ptx = ptxp.newTransaction();
    transactionManager.release(ptx, btpc.getObjects(), btpc.getNewRoots());
    gtxm.commitAll(ptx, btpc.getAppliedServerTransactionIDs());
    gtxm.completeTransactions(ptx, btpc.getCompletedTransactionIDs());
    ptx.commit();
    transactionManager.committed(btpc.getTxns());
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
  }

}
