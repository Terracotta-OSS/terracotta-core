/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.MultiThreadedEventContext;
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
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
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
  private static final int               TXN_LIMIT_COUNT = 500;

  private ServerTransactionManager       transactionManager;
  private LockManager                    lockManager;
  private Sink                           recycleCommitSink;
  private Sink                           broadcastChangesSink;
  private Sink                           evictionInitiateSink;
  private final ObjectInstanceMonitor    instanceMonitor;
  private TransactionalObjectManager     txnObjectMgr;
  private final Timer                    timer = new Timer("Apply Transaction Change Timer", true);

  private volatile GlobalTransactionID   lowWaterMark                    = GlobalTransactionID.NULL_ID;
  private final TransactionProvider      persistenceTransactionProvider;
  private ThreadLocal<CommitContext>     currentCommitContext = new ThreadLocal<CommitContext>();

  public ApplyTransactionChangeHandler(final ObjectInstanceMonitor instanceMonitor, final GlobalTransactionManager gtxm,
                                       TransactionProvider persistenceTransactionProvider) {
    this.instanceMonitor = instanceMonitor;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        lowWaterMark = gtxm.getLowGlobalTransactionIDWatermark();
      }
    }, 0, LWM_UPDATE_INTERVAL);
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof CommitContext) {
      ((CommitContext)context).commit();
      return;
    }

    ApplyTransactionContext atc = (ApplyTransactionContext) context;
    ServerTransaction txn = atc.getTxn();
    ServerTransactionID stxnID = txn.getServerTransactionID();
    ApplyTransactionInfo applyInfo = new ApplyTransactionInfo(txn.isActiveTxn(), stxnID, txn.isSearchEnabled());
    CommitContext commitContext = getCurrentCommitContext(atc);

    if (atc.needsApply()) {
      transactionManager.apply(txn, atc.getObjects(), applyInfo, this.instanceMonitor);
      txnObjectMgr.applyTransactionComplete(applyInfo);
      commitContext.addToCommit(applyInfo.getObjectsToRelease(), txn.getNewRoots(),
          applyInfo.getServerTransactionID(), applyInfo.getObjectIDsToDelete());
    } else {
      transactionManager.skipApplyAndCommit(txn);
      txnObjectMgr.applyTransactionComplete(applyInfo);
      commitContext.releaseOnCommit(applyInfo.getObjectsToRelease());
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

  private CommitContext getCurrentCommitContext(ApplyTransactionContext atc) {
    if (currentCommitContext.get() != null && currentCommitContext.get().shouldCommitNow()) {
      currentCommitContext.get().commit();
    }

    if (currentCommitContext.get() == null || currentCommitContext.get().isCommitted()) {
      currentCommitContext.set(new CommitContext(persistenceTransactionProvider.newTransaction(), atc.getKey()));
      recycleCommitSink.add(currentCommitContext.get());
    }
    return currentCommitContext.get();
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.broadcastChangesSink = scc.getStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE).getSink();
    this.evictionInitiateSink = scc.getStage(ServerConfigurationContext.SERVER_MAP_CAPACITY_EVICTION_STAGE).getSink();
    this.recycleCommitSink = scc.getStage(ServerConfigurationContext.APPLY_CHANGES_STAGE).getSink();
    this.txnObjectMgr = scc.getTransactionalObjectManager();
    this.lockManager = scc.getLockManager();
  }

  private class CommitContext implements MultiThreadedEventContext {
    private final Transaction transaction;
    private final Collection<ManagedObject> objectsToRelease = new ArrayList<ManagedObject>(TXN_LIMIT_COUNT);
    private final Map<String, ObjectID> newRoots = new HashMap<String, ObjectID>();
    private final Collection<ServerTransactionID> serverTransactionIDs = new ArrayList<ServerTransactionID>(TXN_LIMIT_COUNT);
    private final SortedSet<ObjectID> objectsToDelete = new ObjectIDSet();
    private final Object multiThreadKey;

    private int numberOfCommits = 0;
    private boolean committed = false;

    private CommitContext(final Transaction transaction, Object multiThreadKey) {
      this.transaction = transaction;
      this.multiThreadKey = multiThreadKey;
    }

    void releaseOnCommit(Collection<ManagedObject> release) {
      objectsToRelease.addAll(release);
    }

    void addToCommit(Collection<ManagedObject> release, Map<String, ObjectID> roots, ServerTransactionID serverTransactionID,
                          SortedSet<ObjectID> delete) {
      objectsToRelease.addAll(release);
      newRoots.putAll(roots);
      serverTransactionIDs.add(serverTransactionID);
      objectsToDelete.addAll(delete);
      numberOfCommits++;
    }

    boolean shouldCommitNow() {
      return numberOfCommits >= TXN_LIMIT_COUNT;
    }

    boolean isCommitted() {
      return committed;
    }

    void commit() {
      if (committed) {
        return;
      }
      committed = true;
      transaction.commit();
      transactionManager.commit(objectsToRelease, newRoots, serverTransactionIDs, objectsToDelete);
    }

    @Override
    public Object getKey() {
      return multiThreadKey;
    }
  }
}
