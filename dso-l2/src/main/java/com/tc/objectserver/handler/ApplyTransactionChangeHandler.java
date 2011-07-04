/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.locks.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.ServerMapEvictionInitiateContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;

import java.util.Iterator;
import java.util.Set;

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
  private Sink                           evictionInitiateSink;
  private final ObjectInstanceMonitor    instanceMonitor;
  private final GlobalTransactionManager gtxm;
  private TransactionalObjectManager     txnObjectMgr;

  private int                            count                           = 0;
  private GlobalTransactionID            lowWaterMark                    = GlobalTransactionID.NULL_ID;

  public ApplyTransactionChangeHandler(final ObjectInstanceMonitor instanceMonitor, final GlobalTransactionManager gtxm) {
    this.instanceMonitor = instanceMonitor;
    this.gtxm = gtxm;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ApplyTransactionContext atc = (ApplyTransactionContext) context;
    final ServerTransaction txn = atc.getTxn();

    NotifiedWaiters notifiedWaiters = new NotifiedWaiters();
    final ServerTransactionID stxnID = txn.getServerTransactionID();
    final ApplyTransactionInfo applyInfo = new ApplyTransactionInfo(txn.isActiveTxn());

    if (atc.needsApply()) {
      this.transactionManager.apply(txn, atc.getObjects(), applyInfo, this.instanceMonitor);
      this.txnObjectMgr.applyTransactionComplete(stxnID);
    } else {
      this.transactionManager.skipApplyAndCommit(txn);
      getLogger().warn("Not applying previously applied transaction: " + stxnID);
    }

    for (final Iterator i = txn.getNotifies().iterator(); i.hasNext();) {
      final Notify notify = (Notify) i.next();
      final ServerLock.NotifyAction allOrOne = notify.getIsAll() ? ServerLock.NotifyAction.ALL
          : ServerLock.NotifyAction.ONE;
      notifiedWaiters = this.lockManager.notify(notify.getLockID(), (ClientID) txn.getSourceID(), notify.getThreadID(),
                                                allOrOne, notifiedWaiters);
    }

    if (txn.isActiveTxn()) {
      final Set initiateEviction = applyInfo.getObjectIDsToInitateEviction();
      if (!initiateEviction.isEmpty()) {
        this.evictionInitiateSink.add(new ServerMapEvictionInitiateContext(initiateEviction));
      }

      if (this.count == 0) {
        this.lowWaterMark = this.gtxm.getLowGlobalTransactionIDWatermark();
      }
      this.count = this.count++ % LOW_WATER_MARK_UPDATE_FREQUENCY;
      this.broadcastChangesSink.add(new BroadcastChangeContext(txn, this.lowWaterMark, notifiedWaiters, applyInfo));
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.broadcastChangesSink = scc.getStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE).getSink();
    this.evictionInitiateSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_CAPACITY_EVICTION_STAGE).getSink();
    this.txnObjectMgr = scc.getTransactionalObjectManager();
    this.lockManager = scc.getLockManager();
  }
}
