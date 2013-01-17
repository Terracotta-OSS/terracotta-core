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
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.locks.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;
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
import com.tc.util.TCCollections;

import java.util.Collections;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Applies all the changes in a transaction then releases the objects and passes the changes off to be broadcast to the
 * interested client sessions
 * 
 * @author steve
 */
public class ApplyTransactionChangeHandler extends AbstractEventHandler {
  private static final int               LWM_UPDATE_INTERVAL = 10000;

  private ServerTransactionManager       transactionManager;
  private LockManager                    lockManager;
  private Sink                           broadcastChangesSink;
  private Sink                           evictionInitiateSink;
  private final ObjectInstanceMonitor    instanceMonitor;
  private TransactionalObjectManager     txnObjectMgr;

  private volatile GlobalTransactionID   lowWaterMark                    = GlobalTransactionID.NULL_ID;
  private final TransactionProvider      persistenceTransactionProvider;

  private final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();

  public ApplyTransactionChangeHandler(final ObjectInstanceMonitor instanceMonitor, final GlobalTransactionManager gtxm,
                                       TransactionProvider persistenceTransactionProvider) {
    this.instanceMonitor = instanceMonitor;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    new Timer("Apply Transaction Change Timer", true).schedule(new TimerTask() {
      @Override
      public void run() {
        lowWaterMark = gtxm.getLowGlobalTransactionIDWatermark();
      }
    }, 0, LWM_UPDATE_INTERVAL);
  }

  @Override
  public void handleEvent(final EventContext context) {

    ApplyTransactionContext atc = (ApplyTransactionContext) context;
    ServerTransaction txn = atc.getTxn();
    ServerTransactionID stxnID = txn.getServerTransactionID();
    ApplyTransactionInfo applyInfo = new ApplyTransactionInfo(txn.isActiveTxn(), stxnID, txn.isSearchEnabled());

    if (atc.needsApply()) {
      if (currentTransaction.get() == null) {
        currentTransaction.set(persistenceTransactionProvider.newTransaction());
      }
      transactionManager.apply(txn, atc.getObjects(), applyInfo, this.instanceMonitor);
      txnObjectMgr.applyTransactionComplete(applyInfo);
      if (!applyInfo.getObjectsToRelease().isEmpty()) {
        currentTransaction.get().commit();
        currentTransaction.set(null);
      }
      transactionManager.commit(applyInfo.getObjectsToRelease(), txn.getNewRoots(), Collections.singleton(txn.getServerTransactionID()), applyInfo.getObjectIDsToDelete());
    } else {
      transactionManager.skipApplyAndCommit(txn);
      txnObjectMgr.applyTransactionComplete(applyInfo);
      transactionManager.commit(applyInfo.getObjectsToRelease(), Collections.<String, ObjectID>emptyMap(),
          Collections.<ServerTransactionID>emptySet(), TCCollections.EMPTY_OBJECT_ID_SET);
      getLogger().warn("Not applying previously applied transaction: " + stxnID);
    }

    transactionManager.processMetaData(txn, atc.needsApply() ? applyInfo : null);

    NotifiedWaiters notifiedWaiters = new NotifiedWaiters();

    for (final Object o : txn.getNotifies()) {
      final Notify notify = (Notify)o;
      final ServerLock.NotifyAction allOrOne = notify.getIsAll() ? ServerLock.NotifyAction.ALL
          : ServerLock.NotifyAction.ONE;
      notifiedWaiters = lockManager.notify(notify.getLockID(), (ClientID)txn.getSourceID(), notify.getThreadID(),
          allOrOne, notifiedWaiters);
    }

    if (txn.isActiveTxn()) {
      final Set<ObjectID> initiateEviction = applyInfo.getObjectIDsToInitateEviction();
      if (!initiateEviction.isEmpty()) {
        evictionInitiateSink.add(new ServerMapEvictionInitiateContext(initiateEviction));
      }

      broadcastChangesSink.add(new BroadcastChangeContext(txn, lowWaterMark, notifiedWaiters, applyInfo));
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
