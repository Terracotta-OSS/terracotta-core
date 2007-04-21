/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.tx.ServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class L2ObjectStateManagerImpl implements L2ObjectStateManager {

  private static final TCLogger                         logger     = TCLogging
                                                                       .getLogger(L2ObjectStateManagerImpl.class);

  private final ObjectManager                           objectManager;
  private final UnappliedTransactionsInTheSystemMonitor txnMonitor = new UnappliedTransactionsInTheSystemMonitor();
  private final CopyOnWriteArrayMap                     nodes      = new CopyOnWriteArrayMap();
  private final CopyOnWriteArrayList                    listeners  = new CopyOnWriteArrayList();

  public L2ObjectStateManagerImpl(ObjectManager objectManager, ServerTransactionManager transactionManager) {
    this.objectManager = objectManager;
    transactionManager.addTransactionListener(txnMonitor);
  }

  public void registerForL2ObjectStateChangeEvents(L2ObjectStateListener listener) {
    listeners.add(listener);
  }

  private void fireMissingObjectsStateEvent(NodeID nodeID, int missingObjects) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      L2ObjectStateListener l = (L2ObjectStateListener) i.next();
      l.missingObjectsFor(nodeID, missingObjects);
    }
  }

  public int getL2Count() {
    return nodes.size();
  }

  public void removeL2(NodeID nodeID) {
    Object l2State = nodes.remove(nodeID);
    if (l2State == null) {
      logger.warn("L2State Not found for " + nodeID);
    }
  }

  public void addL2(NodeID nodeID, Set oids) {
    L2ObjectStateImpl l2State;
    synchronized (nodes) {
      l2State = (L2ObjectStateImpl) nodes.get(nodeID);
      if (l2State != null) {
        logger.warn("L2State already present for " + nodeID + ". " + l2State
                    + " IGNORING setExistingObjectsList : oids count = " + oids.size());
        return;
      }
      l2State = new L2ObjectStateImpl(nodeID, oids);
      nodes.put(nodeID, l2State);
      txnMonitor.callBackWhenAllCurrentTxnsApplied(l2State);
    }
  }

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count, Sink sink) {
    L2ObjectStateImpl l2State = (L2ObjectStateImpl) nodes.get(nodeID);
    if (l2State != null) {
      return l2State.getSomeObjectsToSyncContext(count, sink);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return null;
    }
  }

  public void close(ManagedObjectSyncContext mosc) {
    L2ObjectStateImpl l2State = (L2ObjectStateImpl) nodes.get(mosc.getNodeID());
    if (l2State != null) {
      l2State.close(mosc);
    } else {
      logger.warn("close() : L2 State Object Not found for " + mosc.getNodeID());
    }
  }

  public Collection getL2ObjectStates() {
    return nodes.values();
  }

  private static final State START         = new State("START");
  private static final State READY_TO_SYNC = new State("READY_TO_SYNC");
  private static final State SYNC_STARTED  = new State("SYNC_STARTED");
  private static final State IN_SYNC       = new State("IN_SYNC");

  private final class L2ObjectStateImpl implements L2ObjectState {

    private final NodeID             nodeID;

    private Set                      missingOids;
    private Map                      missingRoots;
    private Set                      existingOids;

    private volatile State           state          = START;

    private ManagedObjectSyncContext syncingContext = null;

    public L2ObjectStateImpl(NodeID nodeID, Set oids) {
      this.nodeID = nodeID;
      this.existingOids = oids;
    }

    private void close(ManagedObjectSyncContext mosc) {
      Assert.assertTrue(mosc == syncingContext);
      mosc.close();
      if (missingOids.isEmpty()) {
        state = IN_SYNC;
      }
      syncingContext = null;
    }

    private ManagedObjectSyncContext getSomeObjectsToSyncContext(int count, Sink sink) {
      Assert.assertTrue(state == SYNC_STARTED);
      Assert.assertNull(syncingContext);
      if (isRootsMissing()) { return getMissingRootsSynccontext(sink); }
      Set oids = new HashSet(count);
      for (Iterator i = missingOids.iterator(); i.hasNext() && --count > 0;) {
        oids.add(i.next());
        // XXX::FIXME This has to be commented because ObjectIDSet2 doesnt support remove().
        // i.remove();
      }
      missingOids.removeAll(oids); // @see above comment
      syncingContext = new ManagedObjectSyncContext(nodeID, oids, !missingOids.isEmpty(), sink);
      return syncingContext;
    }

    private ManagedObjectSyncContext getMissingRootsSynccontext(Sink sink) {
      missingOids.removeAll(this.missingRoots.values());
      syncingContext = new ManagedObjectSyncContext(nodeID, new HashMap(this.missingRoots), !missingOids.isEmpty(),
                                                    sink);
      this.missingRoots.clear();
      return syncingContext;
    }

    private boolean isRootsMissing() {
      return !this.missingRoots.isEmpty();
    }

    private int computeDiff() {
      this.missingOids = objectManager.getAllObjectIDs();
      this.missingRoots = objectManager.getRootNamesToIDsMap();
      int objectCount = missingOids.size();
      Set missingHere = new HashSet();
      for (Iterator i = existingOids.iterator(); i.hasNext();) {
        Object o = i.next();
        if (!missingOids.remove(o)) {
          missingHere.add(o);
        }
      }
      existingOids = null; // Let GC work for us
      missingRoots.values().retainAll(this.missingOids);
      logger.info(nodeID + " : is missing " + missingOids.size() + " out of " + objectCount
                  + " objects of which missing roots = " + missingRoots.size());
      if (!missingHere.isEmpty()) {
        // XXX:: This is possible because some message (Transaction message with new object creation or object delete
        // message from GC) from previous active reached the other node and not this node and the active crashed
        logger.warn("Object IDs MISSING HERE : " + missingHere.size() + " : " + missingHere);
      }
      int missingCount = missingOids.size();
      if (missingCount == 0) {
        state = IN_SYNC;
      } else {
        state = SYNC_STARTED;
      }
      return missingCount;
    }

    public NodeID getNodeID() {
      return nodeID;
    }

    public String toString() {
      return "L2StateObjectImpl [ " + nodeID + " ] : " + (missingOids != null ? "missing = " + missingOids.size() : "")
             + " state = " + state;
    }

    private void moveToReadyToSyncState() {
      state = READY_TO_SYNC;
      int missingObjects = computeDiff();
      fireMissingObjectsStateEvent(this.nodeID, missingObjects);
    }
  }

  /**
   * TODO::This adds some computational overhead. If found as a performance problem, this functionality could be
   * provided by the ServerTransactionManager itself
   */
  private static class UnappliedTransactionsInTheSystemMonitor implements ServerTransactionListener {

    private final Set unappliedTxns    = new HashSet();
    private final Map pendingCallbacks = new HashMap();

    public synchronized void incomingTransactions(ChannelID cid, Set serverTxnIDs) {
      unappliedTxns.addAll(serverTxnIDs);
    }

    public void callBackWhenAllCurrentTxnsApplied(L2ObjectStateImpl state) {
      boolean callBack = false;
      synchronized (this) {
        if (unappliedTxns.isEmpty()) {
          callBack = true;
        } else {
          Set copy = new HashSet(unappliedTxns);
          logger.info(state + " : Pending Txn Applys to wait :" + copy);
          pendingCallbacks.put(state, copy);
        }
      }
      if (callBack) {
        /**
         * XXX:: callbacks are called outside sync scope so that objectmanager.getAllObjectIDs() call which can take a
         * long time during startup in persisitent mode doesnt hold up incomming transactions.
         */
        state.moveToReadyToSyncState();
      }
    }

    public void transactionApplied(ServerTransactionID stxID) {
      List callBacks = null;
      synchronized (this) {
        unappliedTxns.remove(stxID);
        if (pendingCallbacks.isEmpty()) return;
        for (Iterator i = pendingCallbacks.entrySet().iterator(); i.hasNext();) {
          Entry e = (Entry) i.next();
          Set pendingTxns = (Set) e.getValue();
          pendingTxns.remove(stxID);
          if (pendingTxns.isEmpty()) {
            L2ObjectStateImpl callback = (L2ObjectStateImpl) e.getKey();
            if (callBacks == null) {
              callBacks = new ArrayList(3);
            }
            callBacks.add(callback);
            i.remove();
          }
        }
      }
      if (callBacks != null) {
        /**
         * @see comments above.
         */
        for (Iterator i = callBacks.iterator(); i.hasNext();) {
          L2ObjectStateImpl callback = (L2ObjectStateImpl) i.next();
          callback.moveToReadyToSyncState();
        }
      }
    }

    public void transactionCompleted(ServerTransactionID stxID) {
      // NOP
    }

    public void addResentServerTransactionIDs(Collection stxIDs) {
      // NOP
    }

    public void clearAllTransactionsFor(ChannelID killedClient) {
      // NOP
    }
  }
}
