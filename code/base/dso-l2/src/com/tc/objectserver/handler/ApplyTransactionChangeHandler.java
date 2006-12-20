/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Applies all the changes in a transaction then releases the objects and passes the changes off to be broadcast to the
 * interested client sessions
 * 
 * @author steve
 */
public class ApplyTransactionChangeHandler extends AbstractEventHandler {
  private ServerTransactionManager             transactionManager;
  private LockManager                          lockManager;
  private Sink                                 broadcastChangesSink;
  private Sink                                 commitChangesSink;
  private final ObjectInstanceMonitor          instanceMonitor;
  private final ServerGlobalTransactionManager gtxm;
  private TransactionalObjectManager           txnObjectMgr;

  public ApplyTransactionChangeHandler(ObjectInstanceMonitor instanceMonitor, ServerGlobalTransactionManager gtxm) {
    this.instanceMonitor = instanceMonitor;
    this.gtxm = gtxm;
  }

  public void handleEvent(EventContext context) {
    ApplyTransactionContext atc = (ApplyTransactionContext) context;
    final ServerTransaction txn = atc.getTxn();

    NotifiedWaiters notifiedWaiters = new NotifiedWaiters();
    final ServerTransactionID stxnID = txn.getServerTransactionID();
    final BackReferences includeIDs = new BackReferences();
    GlobalTransactionID gtxnID = gtxm.createGlobalTransactionID(stxnID);

    // XXX::FIXME:: this needsApply() call only returns false after the txn is commited. If we see the same txn twice
    // before commit, then we may apply it twice, though in the current state of affairs that is not possible (unless
    // ofcourse there is a bug !)
    if (gtxm.needsApply(stxnID)) {
      transactionManager.apply(gtxnID, txn, atc.getObjects(), includeIDs, instanceMonitor);
      if (txnObjectMgr.applyTransactionComplete(stxnID)) {
        commitChangesSink.add(new CommitTransactionContext());
      }
    } else {
      transactionManager.skipApplyAndCommit(txn);
      getLogger().warn("Not applying previously applied transaction: " + stxnID);
    }

    for (Iterator i = txn.addNotifiesTo(new LinkedList()).iterator(); i.hasNext();) {
      Notify notify = (Notify) i.next();
      lockManager.notify(notify.getLockID(), txn.getChannelID(), notify.getThreadID(), notify.getIsAll(),
                         notifiedWaiters);
    }

    broadcastChangesSink.add(new BroadcastChangeContext(gtxnID, txn, gtxm.getLowGlobalTransactionIDWatermark(),
                                                        notifiedWaiters, includeIDs));
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.broadcastChangesSink = scc.getStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE).getSink();
    this.commitChangesSink = scc.getStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE).getSink();
    this.txnObjectMgr = scc.getTransactionalObjectManager();
    this.lockManager = scc.getLockManager();
  }
}
