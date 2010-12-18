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
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
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

  public ClientStateManagerImpl(final TCLogger logger) {
    this.logger = logger;
    this.clientStates = new ConcurrentHashMap<NodeID, ClientStateImpl>();
  }

  public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                       final ApplyTransactionInfo applyInfo, final NodeID id,
                                                       final Set<ObjectID> lookupObjectIDs,
                                                       final Set<ObjectID> invalidateObjectIDs) {
    final ClientStateImpl clientState = getClientState(id);
    if (clientState == null) {
      this.logger.warn(": createPrunedChangesAndAddObjectIDTo : Client state is NULL (probably due to disconnect) : "
                       + id);
      return Collections.emptyList();
    }

    clientState.lock();
    try {
      final List<DNA> prunedChanges = new LinkedList<DNA>();

      for (final DNA dna : changes) {
        final ObjectID oid = dna.getObjectID();
        if (clientState.containsReference(oid)) {
          if (dna.isDelta() && !applyInfo.isBroadcastIgnoredFor(oid)) {
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
      clientState.addReferencedChildrenTo(lookupObjectIDs, applyInfo);
      clientState.removeReferencedObjectIDsFrom(lookupObjectIDs);

      addInvalidateObjectIDsTo(clientState, invalidateObjectIDs, applyInfo.getObjectIDsToInvalidate());

      return prunedChanges;
    } finally {
      clientState.unlock();
    }
  }

  private void addInvalidateObjectIDsTo(ClientStateImpl clientState, Set<ObjectID> invalidatedObjectIDsForClient,
                                        Set<ObjectID> invalidatedObjectIDs) {
    if (invalidatedObjectIDs.isEmpty()) return;
    for (ObjectID oid : invalidatedObjectIDs) {
      if (clientState.containsReference(oid)) {
        invalidatedObjectIDsForClient.add(oid);
      }
    }
  }

  public void addReference(final NodeID id, final ObjectID objectID) {
    final ClientStateImpl c = getClientState(id);
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

  public void removeReferences(final NodeID id, final Set<ObjectID> removed) {
    final ClientStateImpl c = getClientState(id);
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

  public boolean hasReference(final NodeID id, final ObjectID objectID) {
    final ClientStateImpl c = getClientState(id);
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

  public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> ids) {
    for (final ClientStateImpl c : this.clientStates.values()) {
      c.lock();
      try {
        c.addReferencedIdsTo(ids);
      } finally {
        c.unlock();
      }
    }
    return ids;
  }

  public void removeReferencedFrom(final NodeID id, final Set<ObjectID> oids) {
    final ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": removeReferencedFrom : Client state is NULL (probably due to disconnect) : " + id);
      return;
    }
    c.lock();
    try {
      final Set<ObjectID> refs = c.getReferences();
      oids.removeAll(refs);
    } finally {
      c.unlock();
    }
  }

  /*
   * returns newly added references
   */
  public Set<ObjectID> addReferences(final NodeID id, final Set<ObjectID> oids) {
    final ClientStateImpl c = getClientState(id);
    if (c == null) {
      this.logger.warn(": addReferences : Client state is NULL (probably due to disconnect) : " + id);
      return Collections.emptySet();
    }
    c.lock();
    try {
      final Set<ObjectID> refs = c.getReferences();
      if (refs.isEmpty()) {
        refs.addAll(oids);
        return oids;
      }

      final Set<ObjectID> newReferences = new HashSet<ObjectID>();
      for (final ObjectID oid : oids) {
        if (refs.add(oid)) {
          newReferences.add(oid);
        }
      }
      return newReferences;
    } finally {
      c.unlock();
    }
  }

  public void shutdownNode(final NodeID waitee) {
    this.clientStates.remove(waitee);
  }

  public void startupNode(final NodeID nodeID) {
    final Object old = this.clientStates.put(nodeID, new ClientStateImpl(nodeID));
    if (old != null) { throw new AssertionError("Client connected before disconnecting : old Client state = " + old); }
  }

  private ClientStateImpl getClientState(final NodeID id) {
    return this.clientStates.get(id);
  }

  public int getReferenceCount(final NodeID nodeID) {
    final ClientStateImpl c = getClientState(nodeID);
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
    final PrettyPrinter rv = out;
    out.print(getClass().getName()).flush();
    out = out.duplicateAndIndent();
    out.indent().print("client states: ").flush();
    out = out.duplicateAndIndent();
    for (final ClientStateImpl c : this.clientStates.values()) {
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

    public ClientStateImpl(final NodeID nodeID) {
      this.nodeID = nodeID;
    }

    public void lock() {
      this.lock.lock();
    }

    public void unlock() {
      this.lock.unlock();
    }

    public void removeReferencedObjectIDsFrom(final Set<ObjectID> lookupObjectIDs) {
      lookupObjectIDs.removeAll(this.managed);
    }

    public void addReferencedChildrenTo(final Set objectIDs, final ApplyTransactionInfo applyInfo) {
      final Set parents = applyInfo.getAllParents();
      parents.retainAll(this.managed);
      applyInfo.addReferencedChildrenTo(objectIDs, parents);
    }

    @Override
    public String toString() {
      return "ClientStateImpl[" + this.nodeID + ", " + this.managed + "]";
    }

    public Set<ObjectID> getReferences() {
      return this.managed;
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      out.print(getClass().getName()).flush();
      out.duplicateAndIndent().indent().print("managed: ").visit(this.managed);
      return out;
    }

    public void addReference(final ObjectID id) {
      this.managed.add(id);
    }

    public boolean containsReference(final ObjectID id) {
      return this.managed.contains(id);
    }

    public void removeReferences(final Set<ObjectID> references) {
      this.managed.removeAll(references);
    }

    public void addReferencedIdsTo(final Set<ObjectID> ids) {
      ids.addAll(this.managed);
    }

    public NodeID getNodeID() {
      return this.nodeID;
    }
  }

}
