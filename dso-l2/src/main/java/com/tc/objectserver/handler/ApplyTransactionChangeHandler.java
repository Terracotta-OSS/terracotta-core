/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.locks.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.BroadcastChangeContext;
import com.tc.objectserver.context.FlushApplyCommitContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.event.ServerEventPublisher;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.locks.NotifiedWaiters;
import com.tc.objectserver.locks.ServerLock;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.server.ServerEvent;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Applies all the changes in a transaction then releases the objects and passes the changes off to be broadcast to the
 * interested client sessions
 * 
 * @author steve
 */
public class ApplyTransactionChangeHandler extends AbstractEventHandler {

  private static final TCLogger          LOGGER                          = TCLogging.getLogger(ApplyTransactionChangeHandler.class);

  private static final int               LWM_UPDATE_INTERVAL = 10000;

  private ServerTransactionManager       transactionManager;
  private LockManager                    lockManager;
  private Sink                           broadcastChangesSink;
  private final ServerMapEvictionManager serverEvictions;
  private final ObjectInstanceMonitor    instanceMonitor;
  private TransactionalObjectManager     txnObjectMgr;

  private volatile GlobalTransactionID   lowWaterMark                    = GlobalTransactionID.NULL_ID;
  private final TransactionProvider      persistenceTransactionProvider;

  private final ThreadLocal<CommitContext> localCommitContext = new ThreadLocal<CommitContext>();
  private GarbageCollectionManager garbageCollectionManager;
  private final ServerEventPublisher serverEventPublisher;

  public ApplyTransactionChangeHandler(final ObjectInstanceMonitor instanceMonitor,
                                       final GlobalTransactionManager gtxm,
                                       final ServerMapEvictionManager evictions,
                                       final TransactionProvider persistenceTransactionProvider,
                                       final TaskRunner taskRunner,
                                       final ServerEventPublisher serverEventPublisher) {
    this.instanceMonitor = instanceMonitor;
    this.serverEvictions = evictions;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.serverEventPublisher = serverEventPublisher;
    final Timer timer = taskRunner.newTimer("Apply Transaction Change Timer");
    timer.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() { lowWaterMark = gtxm.getLowGlobalTransactionIDWatermark(); }
    }, 0, LWM_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  @Override
  public void handleEvent(final EventContext context) {

    begin();

    if (context instanceof FlushApplyCommitContext) {
      commit(((FlushApplyCommitContext)context).getObjectsToRelease(), true);
      return;
    }

    ApplyTransactionContext atc = (ApplyTransactionContext) context;
    ServerTransaction txn = atc.getTxn();
    ServerTransactionID stxnID = txn.getServerTransactionID();
    ApplyTransactionInfo applyInfo = new ApplyTransactionInfo(txn.isActiveTxn(), stxnID, txn.isSearchEnabled());

    if (atc.needsApply()) {
      transactionManager.apply(txn, atc.getObjects(), applyInfo, this.instanceMonitor);
      garbageCollectionManager.deleteObjects(applyInfo.getObjectIDsToDelete(), atc.allCheckedOutObjects());
      txnObjectMgr.applyTransactionComplete(applyInfo);
    } else {
      transactionManager.skipApplyAndCommit(txn);
      txnObjectMgr.applyTransactionComplete(applyInfo);
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
      // only for active
      publishModifications(applyInfo);

      final Set<ObjectID> initiateEviction = applyInfo.getObjectIDsToInitateEviction();
      if (!initiateEviction.isEmpty()) {
        for ( ObjectID oid : initiateEviction ) {
          serverEvictions.scheduleCapacityEviction(oid);
        }
      }

      broadcastChangesSink.add(new BroadcastChangeContext(txn, lowWaterMark, notifiedWaiters, applyInfo));
    }

    commit(atc, applyInfo);
  }

  private void publishModifications(final ApplyTransactionInfo applyInfo) {
    final List<ServerEvent> events = applyInfo.getServerEventRecorder().getEvents();
    if (events != null && !events.isEmpty()) {
      serverEventPublisher.post(events);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(events.size() + " server events have been queued for sending");
        LOGGER.debug(events);
      }
    }
  }

  private void begin() {
    if (localCommitContext.get() == null) {
      localCommitContext.set(new CommitContext());
    }
  }

  private void commit(ApplyTransactionContext atc, ApplyTransactionInfo applyInfo) {
    if (atc.needsApply()) {
      commit(applyInfo.getObjectsToRelease(), atc.getTxn().getNewRoots(), 
              atc.getTxn().getServerTransactionID(), applyInfo.isCommitNow());
    } else {
      commit(applyInfo.getObjectsToRelease(), applyInfo.isCommitNow());
    }
  }

  private void commit(Collection<ManagedObject> objectsToRelease, Map<String, ObjectID> moreRoots,
                      ServerTransactionID stxID, boolean done) {
    if (localCommitContext.get().commit(objectsToRelease, moreRoots, stxID, done)) {
      localCommitContext.set(null);
    }
  }

  private void commit(Collection<ManagedObject> objectsToRelease, boolean done) {
    if (localCommitContext.get().commit(objectsToRelease, done)) {
      localCommitContext.set(null);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.transactionManager = scc.getTransactionManager();
    this.broadcastChangesSink = scc.getStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE).getSink();
    this.txnObjectMgr = scc.getTransactionalObjectManager();
    this.lockManager = scc.getLockManager();
    this.garbageCollectionManager = scc.getGarbageCollectionManager();
  }

  private class CommitContext {
    private final Transaction transaction = persistenceTransactionProvider.newTransaction();
    private final Map<String, ObjectID> newRoots = new HashMap<String, ObjectID>();
    private final Collection<ServerTransactionID> stxIDs = new HashSet<ServerTransactionID>();
    private final Collection<ManagedObject> objectsToRelease = new ArrayList<ManagedObject>();

    boolean commit(Collection<ManagedObject> moreObjectsToRelease, boolean done) {
      objectsToRelease.addAll(moreObjectsToRelease);
      if (done) {
        transaction.commit();
        transactionManager.commit(objectsToRelease, newRoots, stxIDs);
        return true;
      } else {
        return false;
      }
    }

    boolean commit(Collection<ManagedObject> objectsToReleaseParam, Map<String, ObjectID> moreRoots,
                          ServerTransactionID stxID,
                          boolean done) {
      stxIDs.add(stxID);
      newRoots.putAll(moreRoots);
      return commit(objectsToReleaseParam, done);
    }
  }
}
