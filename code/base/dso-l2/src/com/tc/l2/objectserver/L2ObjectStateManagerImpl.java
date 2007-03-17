/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.util.Assert;
import com.tc.util.State;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class L2ObjectStateManagerImpl implements L2ObjectStateManager {

  private static final TCLogger logger = TCLogging.getLogger(L2ObjectStateManagerImpl.class);

  CopyOnWriteArrayMap           nodes  = new CopyOnWriteArrayMap();

  public void removeL2(NodeID nodeID) {
    Object l2State = nodes.remove(nodeID);
    if (l2State == null) {
      logger.warn("L2State Not found for " + nodeID);
    }
  }

  public int addL2WithObjectIDs(NodeID nodeID, Set oids, ObjectManager objectManager) {
    L2ObjectStateimpl l2State;
    synchronized (nodes) {
      l2State = (L2ObjectStateimpl) nodes.get(nodeID);
      if (l2State != null) {
        logger.warn("L2State already present for " + nodeID + ". IGNORING setExistingObjectsList : oids count = "
                    + oids.size());
        return 0;
      }
      nodes.put(nodeID, l2State = new L2ObjectStateimpl(nodeID));
    }
    int missing = l2State.initialize(oids, objectManager);
    return missing;
  }

  public ManagedObjectSyncContext getSomeObjectsToSyncContext(NodeID nodeID, int count, Sink sink) {
    L2ObjectStateimpl l2State = (L2ObjectStateimpl) nodes.get(nodeID);
    if (l2State != null) {
      return l2State.getSomeObjectsToSyncContext(count, sink);
    } else {
      logger.warn("L2 State Object Not found for " + nodeID);
      return null;
    }
  }

  public void close(ManagedObjectSyncContext mosc) {
    L2ObjectStateimpl l2State = (L2ObjectStateimpl) nodes.get(mosc.getNodeID());
    if (l2State != null) {
      l2State.close(mosc);
    } else {
      logger.warn("close() : L2 State Object Not found for " + mosc.getNodeID());
    }
  }

  public List getL2ObjectStates() {
    return Arrays.asList(nodes.valuesArray());
  }

  private static final class L2ObjectStateimpl implements L2ObjectState {

    private final NodeID             nodeID;
    // XXX:: Tracking just the missing Oids is better in terms of memory overhead, but this might lead to difficult race
    // conditions. Rethink !!
    private Set                      missingOids;
    private Map                      missingRoots;

    private State                    state          = UNINITIALIZED;

    private static final State       UNINITIALIZED  = new State("UNINITALIZED");
    private static final State       NOT_IN_SYNC    = new State("NOT_IN_SYNC");
    private static final State       IN_SYNC        = new State("IN_SYNC");

    private ManagedObjectSyncContext syncingContext = null;

    public L2ObjectStateimpl(NodeID nodeID) {
      this.nodeID = nodeID;
    }

    private synchronized void close(ManagedObjectSyncContext mosc) {
      Assert.assertTrue(mosc == syncingContext);
      mosc.close();
      if (missingOids.isEmpty()) {
        state = IN_SYNC;
      }
      syncingContext = null;
    }

    private synchronized ManagedObjectSyncContext getSomeObjectsToSyncContext(int count, Sink sink) {
      Assert.assertTrue(state == NOT_IN_SYNC);
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

    private synchronized int initialize(Set oidsFromL2, ObjectManager objectManager) {
      this.missingOids = objectManager.getAllObjectIDs();
      this.missingRoots = objectManager.getRootNamesToIDsMap();
      int objectCount = missingOids.size();
      Set missingHere = new HashSet();
      for (Iterator i = oidsFromL2.iterator(); i.hasNext();) {
        Object o = i.next();
        if (!missingOids.remove(o)) {
          missingHere.add(o);
        }
      }
      missingRoots.values().retainAll(this.missingOids);
      logger.info(nodeID + " : is missing " + missingOids.size() + " out of " + objectCount + " object of which "
                  + missingRoots.size() + " are roots");
      if (!missingHere.isEmpty()) {
        // XXX:: This is possible because some message (Transaction message with new object creation or object delete
        // message from GC) from previous active reached the other node and not this node and the active crashed
        logger.warn("Object IDs MISSING HERE : " + missingHere.size() + " : " + missingHere);
      }
      int missingCount = missingOids.size();
      if (missingCount == 0) {
        state = IN_SYNC;
      } else {
        state = NOT_IN_SYNC;
      }
      notifyAll();
      return missingCount;
    }

    public NodeID getNodeID() {
      return nodeID;
    }

    public synchronized boolean isInSync() {
      return (state == IN_SYNC);
    }

    public String toString() {
      return "L2StateObjectImpl [ " + nodeID + " ] : missing = " + missingOids.size() + " state = " + state;
    }
  }
}
