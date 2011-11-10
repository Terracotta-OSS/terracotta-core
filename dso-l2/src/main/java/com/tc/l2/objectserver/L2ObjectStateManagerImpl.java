/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.concurrent.CopyOnWriteArrayMap;
import com.tc.util.concurrent.ThrottledTaskExecutor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class L2ObjectStateManagerImpl implements L2ObjectStateManager {

  private static final TCLogger          logger                 = TCLogging.getLogger(L2ObjectStateManagerImpl.class);

  private final ObjectManager            objectManager;
  private final CopyOnWriteArrayMap      nodes                  = new CopyOnWriteArrayMap();
  private final CopyOnWriteArrayList     listeners              = new CopyOnWriteArrayList();
  private final ServerTransactionManager transactionManager;
  private final CopyOnWriteArrayMap      syncExecutorContextMap = new CopyOnWriteArrayMap();
  private final int                      syncMaxPendingMsgs;
  private long                           currentSessionId       = 0;

  public L2ObjectStateManagerImpl(final ObjectManager objectManager, final ServerTransactionManager transactionManager) {
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
    int maxSyncPendingMsgs = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_PENDING_MSGS);
    if (maxSyncPendingMsgs <= 0) {
      logger.warn("Passive Object Sync throttle disabled. ("
                  + TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_PENDING_MSGS + " = " + maxSyncPendingMsgs
                  + ")");
    }
    this.syncMaxPendingMsgs = (maxSyncPendingMsgs <= 0) ? Integer.MAX_VALUE : maxSyncPendingMsgs;
  }

  public void registerForL2ObjectStateChangeEvents(final L2ObjectStateListener listener) {
    this.listeners.add(listener);
  }

  private void fireMissingObjectsStateEvent(final NodeID nodeID, final int missingObjects) {
    for (final Iterator i = this.listeners.iterator(); i.hasNext();) {
      final L2ObjectStateListener l = (L2ObjectStateListener) i.next();
      l.missingObjectsFor(nodeID, missingObjects);
    }
  }

  private void fireObjectSyncCompleteEvent(final NodeID nodeID) {
    for (final Iterator i = this.listeners.iterator(); i.hasNext();) {
      final L2ObjectStateListener l = (L2ObjectStateListener) i.next();
      l.objectSyncCompleteFor(nodeID);
    }
  }

  public int getL2Count() {
    return this.nodes.size();
  }

  public void removeL2(final NodeID nodeID) {
    final Object l2State = this.nodes.remove(nodeID);
    if (l2State == null) {
      logger.warn("L2State Not found for " + nodeID);
    }
    this.syncExecutorContextMap.remove(nodeID);
  }

  public boolean addL2(final NodeID nodeID, final Set oids) {
    L2ObjectStateImpl l2State;
    synchronized (this.nodes) {
      l2State = (L2ObjectStateImpl) this.nodes.get(nodeID);
      if (l2State != null) {
        logger.warn("L2State already present for " + nodeID + ". " + l2State
                    + " IGNORING setExistingObjectsList : oids count = " + oids.size());
        return false;
      }
      l2State = new L2ObjectStateImpl(nodeID, oids, this.currentSessionId++);
      this.nodes.put(nodeID, l2State);
    }
    final L2ObjectStateImpl _l2State = l2State;
    this.transactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        _l2State.moveToReadyToSyncState();
      }
    });
    return true;
  }

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(final NodeID nodeID, final int count) {
    final L2ObjectStateImpl l2State = (L2ObjectStateImpl) this.nodes.get(nodeID);
    if (l2State != null) {
      return l2State.getSomeObjectsToSyncContext(count);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return null;
    }
  }

  public void close(final ManagedObjectSyncContext mosc) {
    final L2ObjectStateImpl l2State = (L2ObjectStateImpl) this.nodes.get(mosc.getNodeID());
    if (l2State != null) {
      l2State.close(mosc);
    } else {
      logger.warn("close() : L2 State Object Not found for " + mosc.getNodeID());
    }
  }

  public Collection getL2ObjectStates() {
    return this.nodes.values();
  }

  public void initiateSync(NodeID nodeID, Runnable syncRunnable) {
    ThrottledTaskExecutor throttledTaskExecutor = new ThrottledTaskExecutor(syncMaxPendingMsgs);
    SyncExecutorContext passiveSyncContext = new SyncExecutorContext(throttledTaskExecutor, syncRunnable);
    Object o = this.syncExecutorContextMap.put(nodeID, passiveSyncContext);
    if (o != null) {
      logger.warn("initiateSync: Passive Sync Context already available for " + nodeID);
    }
    syncPassive(throttledTaskExecutor, syncRunnable);
  }

  public void syncMore(NodeID nodeID) {
    SyncExecutorContext passiveSync = (SyncExecutorContext) this.syncExecutorContextMap.get(nodeID);
    if (passiveSync != null) {
      syncPassive(passiveSync.getExecutor(), passiveSync.getRunnable());
    } else {
      logger.warn("syncMore: Passive Sync Context missing for " + nodeID);
    }
  }

  private void syncPassive(ThrottledTaskExecutor executor, Runnable syncRunnable) {
    executor.offer(syncRunnable);
  }

  public void ackSync(NodeID nodeID) {
    SyncExecutorContext passiveSync = (SyncExecutorContext) this.syncExecutorContextMap.get(nodeID);
    if (passiveSync != null) {
      passiveSync.getExecutor().receiveFeedback();
    } else {
      logger.warn("ackSync: Passive Sync Context missing for " + nodeID);
    }
  }

  @Override
  public String toString() {
    StringBuilder strBuilder = new StringBuilder();
    strBuilder.append(L2ObjectStateManagerImpl.class.getSimpleName()).append(": [ ").append(this.nodes.values())
        .append("]");
    return strBuilder.toString();
  }

  private static class SyncExecutorContext {
    private final ThrottledTaskExecutor executor;
    private final Runnable              runnable;

    public SyncExecutorContext(ThrottledTaskExecutor executor, Runnable runnable) {
      this.executor = executor;
      this.runnable = runnable;
    }

    public ThrottledTaskExecutor getExecutor() {
      return executor;
    }

    public Runnable getRunnable() {
      return runnable;
    }

  }

  private static final State START                  = new State("START");
  private static final State READY_TO_SYNC          = new State("READY_TO_SYNC");
  private static final State SYNC_STARTED           = new State("SYNC_STARTED");
  private static final State IN_SYNC_PENDING_NOTIFY = new State("IN_SYNC_PENDING_NOTIFY");
  private static final State IN_SYNC                = new State("IN_SYNC");

  private final class L2ObjectStateImpl implements L2ObjectState {

    private final NodeID             nodeID;

    private ObjectIDSet              missingOids;
    private Map                      missingRoots;
    private Set<ObjectID>            existingOids;

    private volatile State           state          = START;

    private ManagedObjectSyncContext syncingContext = null;

    private int                      totalObjectsToSync;
    private int                      totalObjectsSynced;
    private final long               sessionId;

    public L2ObjectStateImpl(final NodeID nodeID, final Set<ObjectID> oids, final long currentSessionId) {
      this.nodeID = nodeID;
      this.existingOids = oids;
      this.sessionId = currentSessionId;
    }

    private void close(final ManagedObjectSyncContext mosc) {
      if (this.sessionId != mosc.getSessionId()) {
        logger.warn("An old request for object sync for " + this.nodeID + " is being ignored");
        return;
      }
      if (mosc != this.syncingContext) { throw new AssertionError("expected: " + this.syncingContext + " actual: "
                                                                  + mosc); }
      this.syncingContext = null;
      // NotSynchedOids are picked up first as its a stored set and thus prefetching that happened is not a waste.
      missingOids.addAll(mosc.getNotSynchedOids());
      totalObjectsSynced += mosc.getSynchedOids().size();
      if (this.missingOids.isEmpty()) {
        this.state = IN_SYNC_PENDING_NOTIFY;
        L2ObjectStateManagerImpl.this.transactionManager
            .callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
              public void onCompletion() {
                moveToInSyncState();
              }
            });
      }
    }

    private ManagedObjectSyncContext getSomeObjectsToSyncContext(final int count) {
      Assert.assertTrue(this.state == SYNC_STARTED);
      Assert.assertNull(this.syncingContext);
      if (isRootsMissing()) { return getMissingRootsSynccontext(); }
      final ObjectIDSet oids = new ObjectIDSet();
      addSomeMissingObjectIDsTo(oids, count);
      this.syncingContext = new ManagedObjectSyncContext(this.nodeID, oids, !this.missingOids.isEmpty(),
                                                         this.totalObjectsToSync, this.totalObjectsSynced,
                                                         this.sessionId);
      return this.syncingContext;
    }

    private void addSomeMissingObjectIDsTo(final ObjectIDSet oids, int count) {
      for (final Iterator<ObjectID> i = this.missingOids.iterator(); i.hasNext() && --count >= 0;) {
        oids.add(i.next());
        i.remove();
      }
    }

    private ManagedObjectSyncContext getMissingRootsSynccontext() {
      final ObjectIDSet oids = new ObjectIDSet();
      // NOTE:: some root IDs might not be present in this mirror group in AA config
      for (final Iterator i = this.missingRoots.values().iterator(); i.hasNext();) {
        final ObjectID rootID = (ObjectID) i.next();
        if (this.missingOids.remove(rootID)) {
          oids.add(rootID);
        }
      }
      if (oids.isEmpty()) {
        // Get some objects anyways
        addSomeMissingObjectIDsTo(oids, this.missingRoots.size());
      }
      this.syncingContext = new ManagedObjectSyncContext(this.nodeID, new HashMap(this.missingRoots), oids,
                                                         !this.missingOids.isEmpty(), this.totalObjectsToSync,
                                                         this.totalObjectsSynced, this.sessionId);
      this.missingRoots.clear();
      return this.syncingContext;
    }

    private boolean isRootsMissing() {
      return !this.missingRoots.isEmpty();
    }

    private int computeDiff() {
      this.missingOids = L2ObjectStateManagerImpl.this.objectManager.getAllObjectIDs();
      this.missingRoots = L2ObjectStateManagerImpl.this.objectManager.getRootNamesToIDsMap();
      final int objectCount = this.missingOids.size();
      final ObjectIDSet missingHere = new ObjectIDSet();
      for (ObjectID o : this.existingOids) {
        if (!this.missingOids.remove(o)) {
          missingHere.add(o);
        }
      }
      this.totalObjectsToSync = this.missingOids.size();
      // NOTE :: Missing roots is calculated slightly differently to accommodate AA config, where all roots are present
      // in the coordinator but not necessarily all root objects. Also it is possible that existing root id mapping are
      // resent in partially synced passives case in AA since exisitingOids wont contain oid of other mirror groups. But
      // this should not have any adverse effect as the are just rewritten on the passive end.
      this.missingRoots.values().removeAll(this.existingOids);
      this.existingOids = null; // Let DGC work for us
      logger.info(this.nodeID + " : is missing " + this.missingOids.size() + " out of " + objectCount
                  + " objects of which missing roots = " + this.missingRoots.size());
      if (!missingHere.isEmpty()) {
        // XXX:: This is possible because some message (Transaction message with new object creation or object delete
        // message from DGC) from previous active reached the other node and not this node and the active crashed
        logger.warn("Object IDs MISSING HERE : " + missingHere.size() + " : " + missingHere.toShortString());
      }
      final int missingCount = this.missingOids.size();
      if (missingCount == 0) {
        this.state = IN_SYNC;
      } else {
        this.state = SYNC_STARTED;
      }
      return missingCount;
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }

    @Override
    public String toString() {
      return "L2StateObjectImpl [ " + this.nodeID + " ] : "
             + (this.missingOids != null ? "missing = " + this.missingOids.size() : "") + " state = " + this.state;
    }

    private void moveToReadyToSyncState() {
      this.state = READY_TO_SYNC;
      final int missingObjects = computeDiff();
      fireMissingObjectsStateEvent(this.nodeID, missingObjects);
    }

    private void moveToInSyncState() {
      this.state = IN_SYNC;
      fireObjectSyncCompleteEvent(this.nodeID);
    }
  }
}
