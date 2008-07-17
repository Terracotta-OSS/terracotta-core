/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author steve
 */
public class ClientStateManagerImpl implements ClientStateManager {

  private static final State STARTED = new State("STARTED");
  private static final State STOPPED = new State("STOPPED");

  private State              state   = STARTED;

  private final Map<NodeID, ClientState>  clientStates;
  private final TCLogger                  logger;

  // for testing
  public ClientStateManagerImpl(TCLogger logger, Map<NodeID, ClientState> states) {
    this.logger = logger;
    this.clientStates = states;
  }

  public ClientStateManagerImpl(TCLogger logger) {
    this(logger, new HashMap<NodeID, ClientState>());
  }

  public synchronized List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, BackReferences includeIDs,
                                                               NodeID id, Set<ObjectID> objectIDs) {
    assertStarted();
    ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptyList();
    }

    List<DNA> prunedChanges = new LinkedList<DNA>();

    for (final DNA dna : changes) {
      if (clientState.containsReference(dna.getObjectID())) {
        if (dna.isDelta()) {
          prunedChanges.add(dna);
        } else {
          // This new Object must have already been sent as a part of a different lookup. So ignoring this change.
        }
        // } else if (clientState.containsParent(dna.getObjectID(), includeIDs)) {
        // these objects needs to be looked up from the client during apply
        // objectIDs.add(dna.getObjectID());
      }
    }
    clientState.addReferencedChildrenTo(objectIDs, includeIDs);

    return prunedChanges;
  }

  public synchronized void addReference(NodeID id, ObjectID objectID) {
    assertStarted();
    // logger.info("Adding Reference for " + id + " to " + objectID);
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.addReference(objectID);
    } else {
      logger.warn(": addReference : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized void removeReferences(NodeID id, Set<ObjectID> removed) {
    assertStarted();
    // logger.info("Removing Reference for " + id + " to " + removed);
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.removeReferences(removed);
    } else {
      logger.warn(": removeReferences : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public synchronized boolean hasReference(NodeID id, ObjectID objectID) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      return c.containsReference(objectID);
    } else {
      logger.warn(": hasReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
  }

  public synchronized void addAllReferencedIdsTo(Set<ObjectID> ids) {
    assertStarted();
    for (final ClientState s : clientStates.values()) {
      s.addReferencedIdsTo(ids);
    }
  }

  public synchronized void removeReferencedFrom(NodeID id, Set<ObjectID> oids) {
    ClientState cs = getClientState(id);
    if (cs == null) {
      logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
    Set<ObjectID> refs = cs.getReferences();
    // XXX:: This is a work around for THashSet's poor implementation of removeAll
    if (oids.size() >= refs.size()) {
      oids.removeAll(refs);
    } else {
      for (Iterator<ObjectID> i = oids.iterator(); i.hasNext();) {
        if (refs.contains(i.next())) {
          i.remove();
        }
      }
    }

  }

  /*
   * returns newly added references
   */
  public synchronized Set<ObjectID> addReferences(NodeID id, Set<ObjectID> oids) {
    ClientState cs = getClientState(id);
    if (cs == null) {
      logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptySet();
    }
    Set<ObjectID> refs = cs.getReferences();
    if (refs.isEmpty()) {
      refs.addAll(oids);
      return oids;
    }
    Set<ObjectID> newReferences = new HashSet<ObjectID>();
    for (ObjectID oid : oids) {
      if (refs.add(oid)) {
        newReferences.add(oid);
      }
    }
    return newReferences;
  }

  public synchronized void shutdownNode(NodeID waitee) {
    if (!isStarted()) {
      // it's too late to remove the client from the database. On startup, this guy will fail to reconnect
      // within the timeout period and be slain.
      return;
    }
    clientStates.remove(waitee);
  }

  public synchronized void startupNode(NodeID nodeID) {
    if (!isStarted()) { return; }
    Object old = clientStates.put(nodeID, new ClientStateImpl(nodeID));
    if (old != null) { throw new AssertionError("Client connected before disconnecting : old Client state = " + old); }
  }

  public synchronized void stop() {
    assertStarted();
    state = STOPPED;
    logger.info("ClientStateManager stopped.");
  }

  private boolean isStarted() {
    return state == STARTED;
  }

  private void assertStarted() {
    if (state != STARTED) throw new AssertionError("Not started.");
  }

  private ClientStateImpl getClientState(NodeID id) {
    return (ClientStateImpl) clientStates.get(id);
  }

  public int getReferenceCount(NodeID nodeID) {
    ClientState clientState = getClientState(nodeID);
    return clientState != null ? clientState.getReferences().size() : 0;
  }

  public synchronized Set<NodeID> getConnectedClientIDs() {
    return clientStates.keySet();
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.println(getClass().getName());
    out = out.duplicateAndIndent();
    out.indent().println("client states: ");
    out = out.duplicateAndIndent();
    for (NodeID key : clientStates.keySet()) {
      ClientState st = clientStates.get(key);
      out.indent().print(key + "=").visit(st).println();
    }
    return rv;
  }

  private static class ClientStateImpl implements PrettyPrintable, ClientState {
    private final NodeID        nodeID;
    private final Set<ObjectID> managed = new ObjectIDSet();

    public ClientStateImpl(NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public void addReferencedChildrenTo(Set objectIDs, BackReferences includeIDs) {
      Set parents = includeIDs.getAllParents();
      parents.retainAll(managed);
      includeIDs.addReferencedChildrenTo(objectIDs, parents);
    }

    public String toString() {
      return "ClientStateImpl[" + nodeID + ", " + managed + "]";
    }

    public Set<ObjectID> getReferences() {
      return managed;
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.println(getClass().getName());
      out.duplicateAndIndent().indent().print("managed: ").visit(managed);
      return out;
    }

    public void addReference(ObjectID id) {
      managed.add(id);
    }

    public boolean containsReference(ObjectID id) {
      return managed.contains(id);
    }

    public void removeReferences(Set<ObjectID> references) {
      managed.removeAll(references);
    }

    public void addReferencedIdsTo(Set<ObjectID> ids) {
      ids.addAll(managed);
    }

    public NodeID getNodeID() {
      return nodeID;
    }
  }

}
