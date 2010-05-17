/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.l1.api.ClientState;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client State Manager maintains the list of objects that are faulted into each client.
 */
public class ClientStateManagerImpl implements ClientStateManager, PrettyPrintable {

  private final Map<NodeID, ClientStateImpl> clientStates;
  private final TCLogger                     logger;

  public ClientStateManagerImpl(TCLogger logger) {
    this.logger = logger;
    this.clientStates = new ConcurrentHashMap<NodeID, ClientStateImpl>();
  }

  public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, BackReferences includeIDs, NodeID id,
                                                       Set<ObjectID> lookupObjectIDs) {
    ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      this.logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : "
                       + id);
      return Collections.emptyList();
    }

    clientState.lock();
    try {
      List<DNA> prunedChanges = new LinkedList<DNA>();

      for (final DNA dna : changes) {
        if (clientState.containsReference(dna.getObjectID())) {
          if (dna.isDelta()) {
            prunedChanges.add(dna);
          } else {
            // This new Object must have already been sent as a part of a different lookup. So ignoring this change.
          }
          // else if (clientState.containsParent(dna.getObjectID(), includeIDs)) {
          // these objects needs to be looked up from the client during apply
          // objectIDs.add(dna.getObjectID());
          // }
        }
      }
      clientState.addReferencedChildrenTo(lookupObjectIDs, includeIDs);
      clientState.removeReferencedObjectIDsFrom(lookupObjectIDs);

      return prunedChanges;
    } finally {
      clientState.unlock();
    }
  }

  public void addReference(NodeID id, ObjectID objectID) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        c.addReference(objectID);
      } finally {
        c.unlock();
      }
    } else {
      this.logger.warn(": addReference : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public void removeReferences(NodeID id, Set<ObjectID> removed) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        c.removeReferences(removed);
      } finally {
        c.unlock();
      }
    } else {
      this.logger.warn(": removeReferences : Client state is NULL (probably due to disconnect) : " + id);
    }
  }

  public boolean hasReference(NodeID id, ObjectID objectID) {
    ClientStateImpl c = getClientState(id);
    if (c != null) {
      c.lock();
      try {
        return c.containsReference(objectID);
      } finally {
        c.unlock();
      }
    } else {
      this.logger.warn(": hasReference : Client state is NULL (probably due to disconnect) : " + id);
      return false;
    }
  }

  public void addAllReferencedIdsTo(Set<ObjectID> ids) {
    for (final ClientStateImpl c : this.clientStates.values()) {
      c.lock();
      try {
        c.addReferencedIdsTo(ids);
      } finally {
        c.unlock();
      }
    }
  }

  public void removeReferencedFrom(NodeID id, Set<ObjectID> oids) {
    ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
    c.lock();
    try {
      Set<ObjectID> refs = c.getReferences();
      oids.removeAll(refs);
    } finally {
      c.unlock();
    }
  }

  /*
   * returns newly added references
   */
  public Set<ObjectID> addReferences(NodeID id, Set<ObjectID> oids) {
    ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptySet();
    }
    c.lock();
    try {
      Set<ObjectID> refs = c.getReferences();
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
    } finally {
      c.unlock();
    }
  }

  public void shutdownNode(NodeID waitee) {
    this.clientStates.remove(waitee);
  }

  public void startupNode(NodeID nodeID) {
    Object old = this.clientStates.put(nodeID, new ClientStateImpl(nodeID));
    if (old != null) { throw new AssertionError("Client connected before disconnecting : old Client state = " + old); }
  }

  private ClientStateImpl getClientState(NodeID id) {
    return this.clientStates.get(id);
  }

  public int getReferenceCount(NodeID nodeID) {
    ClientStateImpl c = getClientState(nodeID);
    if (c == null) { return 0; }
    c.lock();
    try {
      return c.getReferences().size();
    } finally {
      c.unlock();
    }
  }

  public Set<NodeID> getConnectedClientIDs() {
    return Collections.unmodifiableSet(this.clientStates.keySet());
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out.print(getClass().getName()).flush();
    out = out.duplicateAndIndent();
    out.indent().print("client states: ").flush();
    out = out.duplicateAndIndent();
    for (ClientStateImpl c : this.clientStates.values()) {
      c.lock();
      try {
        out.indent().print(c.getNodeID() + "=").visit(c).flush();
      } finally {
        c.unlock();
      }
    }
    return rv;
  }

  private static class ClientStateImpl implements PrettyPrintable, ClientState {
    private final NodeID        nodeID;
    private final Set<ObjectID> managed = new ObjectIDSet();
    private final ReentrantLock lock    = new ReentrantLock();

    public ClientStateImpl(NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public void lock() {
      this.lock.lock();
    }

    public void unlock() {
      this.lock.unlock();
    }

    public void removeReferencedObjectIDsFrom(Set<ObjectID> lookupObjectIDs) {
      lookupObjectIDs.removeAll(this.managed);
    }

    public void addReferencedChildrenTo(Set objectIDs, BackReferences includeIDs) {
      Set parents = includeIDs.getAllParents();
      parents.retainAll(this.managed);
      includeIDs.addReferencedChildrenTo(objectIDs, parents);
    }

    @Override
    public String toString() {
      return "ClientStateImpl[" + this.nodeID + ", " + this.managed + "]";
    }

    public Set<ObjectID> getReferences() {
      return this.managed;
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.print(getClass().getName()).flush();
      out.duplicateAndIndent().indent().print("managed: ").visit(this.managed);
      return out;
    }

    public void addReference(ObjectID id) {
      this.managed.add(id);
    }

    public boolean containsReference(ObjectID id) {
      return this.managed.contains(id);
    }

    public void removeReferences(Set<ObjectID> references) {
      this.managed.removeAll(references);
    }

    public void addReferencedIdsTo(Set<ObjectID> ids) {
      ids.addAll(this.managed);
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }
  }

}
