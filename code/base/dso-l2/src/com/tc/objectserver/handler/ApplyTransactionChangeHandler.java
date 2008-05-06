/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.NotifiedWaiters;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.Iterator;

/**
 * Applies all the changes in a transaction then releases the objects and passes the changes off to be broadcast to the
 * interested client sessions
 * 
 * @author steve
 */
public class ApplyTransactionChangeHandler extends AbstractEventHandler {

  // Every 100 transactions, it updates the LWM
  private static final int               LOW_WATER_MARK_UPDATE_FREQUENCY = 100;

  private ServerTransactionManager       transactionManager;
  private LockManager                    lockManager;
  private Sink                           broadcastChangesSink;
  private final ObjectInstanceMonitor    instanceMonitor;
  private final GlobalTransactionManager gtxm;
  private TransactionalObjectManager     txnObjectMgr;

  private int                            count                           = 0;
  private GlobalTransactionID            lowWaterMark                    = GlobalTransactionID.NULL_ID;

  public ApplyTransactionChangeHandler(ObjectInstanceMonitor instanceMonitor, GlobalTransactionManager gtxm) {
    this.instanceMonitor = instanceMonitor;
    this.gtxm = gtxm;
  }

  public void handleEvent(EventContext context) {
    ApplyTransactionContext atc = (ApplyTransactionContext) context;
    final ServerTransaction txn = atc.getTxn();

    NotifiedWaiters notifiedWaiters = new NotifiedWaiters();
    final ServerTransactionID stxnID = txn.getServerTransactionID();
    final BackReferences includeIDs = new BackReferences();

    if (atc.needsApply()) {
      transactionManager.apply(txn, atc.getObjects(), includeIDs, instanceMonitor);
      txnObjectMgr.applyTransactionComplete(stxnID);
    } else {
      transactionManager.skipApplyAndCommit(txn);
      getLogger().warn("Not applying previously applied transaction: " + stxnID);
    }

    for (Iterator i = txn.getNotifies().iterator(); i.hasNext();) {
      Notify notify = (Notify) i.next();
      lockManager.notify(notify.getLockID(), txn.getSourceID(), notify.getThreadID(), notify.getIsAll(),
                         notifiedWaiters);
    }

    if (txn.needsBroadcast()) {
      if (count == 0) {
        lowWaterMark = gtxm.getLowGlobalTransactionIDWatermark();
      }
      count = count++ % LOW_WATER_MARK_UPDATE_FREQUENCY;
      broadcastChangesSink.add(new BroadcastChangeContext(txn, lowWaterMark, notifiedWaiters, includeIDs));
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.broadcastChangesSink = scc.getStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE).getSink();
    this.txnObjectMgr = scc.getTransactionalObjectManager();
    this.lockManager = scc.getLockManager();
  }
}
