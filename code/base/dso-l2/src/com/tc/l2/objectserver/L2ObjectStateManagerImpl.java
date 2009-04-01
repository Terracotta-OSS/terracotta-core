/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionLister;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class L2ObjectStateManagerImpl implements L2ObjectStateManager {

  private static final TCLogger          logger    = TCLogging.getLogger(L2ObjectStateManagerImpl.class);

  private final ObjectManager            objectManager;
  private final CopyOnWriteArrayMap      nodes     = new CopyOnWriteArrayMap();
  private final CopyOnWriteArrayList     listeners = new CopyOnWriteArrayList();
  private final ServerTransactionManager transactionManager;

  public L2ObjectStateManagerImpl(ObjectManager objectManager, ServerTransactionManager transactionManager) {
    this.objectManager = objectManager;
    this.transactionManager = transactionManager;
  }

  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener) {
    this.listeners.add(listener);
  }

  private void fireMissingObjectsStateEvent(NodeID nodeID, int missingObjects) {
    for (Iterator i = this.listeners.iterator(); i.hasNext();) {
      L2ObjectStateListener l = (L2ObjectStateListener) i.next();
      l.missingObjectsFor(nodeID, missingObjects);
    }
  }

  private void fireObjectSyncCompleteEvent(NodeID nodeID) {
    for (Iterator i = this.listeners.iterator(); i.hasNext();) {
      L2ObjectStateListener l = (L2ObjectStateListener) i.next();
      l.objectSyncCompleteFor(nodeID);
    }
  }

  public int getL2Count() {
    return this.nodes.size();
  }

  public void removeL2(NodeID nodeID) {
    Object l2State = this.nodes.remove(nodeID);
    if (l2State == null) {
      logger.warn("L2State Not found for " + nodeID);
    }
  }

  public boolean addL2(NodeID nodeID, Set oids) {
    L2ObjectStateImpl l2State;
    synchronized (this.nodes) {
      l2State = (L2ObjectStateImpl) this.nodes.get(nodeID);
      if (l2State != null) {
        logger.warn("L2State already present for " + nodeID + ". " + l2State
                    + " IGNORING setExistingObjectsList : oids count = " + oids.size());
        return false;
      }
      l2State = new L2ObjectStateImpl(nodeID, oids);
      this.nodes.put(nodeID, l2State);
    }
    final L2ObjectStateImpl _l2State = l2State;
    this.transactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionLister() {
      public void onCompletion() {
        _l2State.moveToReadyToSyncState();
      }
    });
    return true;
  }

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count, Sink sink) {
    L2ObjectStateImpl l2State = (L2ObjectStateImpl) this.nodes.get(nodeID);
    if (l2State != null) {
      return l2State.getSomeObjectsToSyncContext(count, sink);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return null;
    }
  }

  public void close(ManagedObjectSyncContext mosc) {
    L2ObjectStateImpl l2State = (L2ObjectStateImpl) this.nodes.get(mosc.getNodeID());
    if (l2State != null) {
      l2State.close(mosc);
    } else {
      logger.warn("close() : L2 State Object Not found for " + mosc.getNodeID());
    }
  }

  public Collection getL2ObjectStates() {
    return this.nodes.values();
  }

  private static final State START                  = new State("START");
  private static final State READY_TO_SYNC          = new State("READY_TO_SYNC");
  private static final State SYNC_STARTED           = new State("SYNC_STARTED");
  private static final State IN_SYNC_PENDING_NOTIFY = new State("IN_SYNC_PENDING_NOTIFY");
  private static final State IN_SYNC                = new State("IN_SYNC");

  private final class L2ObjectStateImpl implements L2ObjectState {

    private final NodeID             nodeID;

    private Set                      missingOids;
    private Map                      missingRoots;
    private Set                      existingOids;

    private volatile State           state          = START;

    private ManagedObjectSyncContext syncingContext = null;

    private int                      totalObjectsToSync;
    private int                      totalObjectsSynced;

    public L2ObjectStateImpl(NodeID nodeID, Set oids) {
      this.nodeID = nodeID;
      this.existingOids = oids;
    }

    private void close(ManagedObjectSyncContext mosc) {
      Assert.assertTrue(mosc == this.syncingContext);
      this.syncingContext = null;
      if (this.missingOids.isEmpty()) {
        this.state = IN_SYNC_PENDING_NOTIFY;
        L2ObjectStateManagerImpl.this.transactionManager
            .callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionLister() {
              public void onCompletion() {
                moveToInSyncState();
              }
            });
      }
    }

    private ManagedObjectSyncContext getSomeObjectsToSyncContext(int count, Sink sink) {
      Assert.assertTrue(this.state == SYNC_STARTED);
      Assert.assertNull(this.syncingContext);
      if (isRootsMissing()) { return getMissingRootsSynccontext(sink); }
      ObjectIDSet oids = new ObjectIDSet();
      addSomeMissingObjectIDsTo(oids, count);
      this.totalObjectsSynced += oids.size();
      this.syncingContext = new ManagedObjectSyncContext(this.nodeID, oids, !this.missingOids.isEmpty(), sink,
                                                         this.totalObjectsToSync, this.totalObjectsSynced);
      return this.syncingContext;
    }

    private void addSomeMissingObjectIDsTo(ObjectIDSet oids, int count) {
      for (Iterator i = this.missingOids.iterator(); i.hasNext() && --count >= 0;) {
        oids.add(i.next());
        // XXX:: This has to be commented because even though ObjectIDSet supports remove() now it is slightly slower
        // than removeAll.
        // i.remove();
      }
      this.missingOids.removeAll(oids); // @see above comment

    }

    private ManagedObjectSyncContext getMissingRootsSynccontext(Sink sink) {
      ObjectIDSet oids = new ObjectIDSet();
      // NOTE:: some root IDs might not be present in this mirror group in AA config
      for (Iterator i = this.missingRoots.values().iterator(); i.hasNext();) {
        ObjectID rootID = (ObjectID) i.next();
        if (this.missingOids.remove(rootID)) {
          oids.add(rootID);
        }
      }
      if (oids.isEmpty()) {
        // Get some objects anyways
        addSomeMissingObjectIDsTo(oids, this.missingRoots.size());
      }
      this.totalObjectsSynced += oids.size();
      this.syncingContext = new ManagedObjectSyncContext(this.nodeID, new HashMap(this.missingRoots), oids,
                                                         !this.missingOids.isEmpty(), sink, this.totalObjectsToSync,
                                                         this.totalObjectsSynced);
      this.missingRoots.clear();
      return this.syncingContext;
    }

    private boolean isRootsMissing() {
      return !this.missingRoots.isEmpty();
    }

    private int computeDiff() {
      this.missingOids = L2ObjectStateManagerImpl.this.objectManager.getAllObjectIDs();
      this.missingRoots = L2ObjectStateManagerImpl.this.objectManager.getRootNamesToIDsMap();
      int objectCount = this.missingOids.size();
      Set missingHere = new HashSet();
      for (Iterator i = this.existingOids.iterator(); i.hasNext();) {
        Object o = i.next();
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
        logger.warn("Object IDs MISSING HERE : " + missingHere.size() + " : " + missingHere);
      }
      int missingCount = this.missingOids.size();
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
      int missingObjects = computeDiff();
      fireMissingObjectsStateEvent(this.nodeID, missingObjects);
    }

    private void moveToInSyncState() {
      this.state = IN_SYNC;
      fireObjectSyncCompleteEvent(this.nodeID);
    }
  }
}
