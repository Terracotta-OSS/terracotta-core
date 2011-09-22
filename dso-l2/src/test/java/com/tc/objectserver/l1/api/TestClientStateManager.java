/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TestClientStateManager implements ClientStateManager {

  public NodeID                    shutdownClient    = null;
  public List<AddReferenceContext> addReferenceCalls = new ArrayList<AddReferenceContext>();

  public void shutdownNode(final NodeID deadNode) {
    this.shutdownClient = deadNode;
  }

  public void addReference(final NodeID nodeID, final ObjectID objectID) {
    this.addReferenceCalls.add(new AddReferenceContext(nodeID, objectID));
  }

  public static class AddReferenceContext {
    public final NodeID   nodeID;
    public final ObjectID objectID;

    private AddReferenceContext(final NodeID nodeID, final ObjectID objectID) {
      this.nodeID = nodeID;
      this.objectID = objectID;
    }
  }

  public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
    //
  }

  public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                       final ApplyTransactionInfo includeIDs, final NodeID clientID,
                                                       final Set<ObjectID> objectIDs, Invalidations invalidIDs) {
    return Collections.emptyList();
  }

  public boolean hasReference(final NodeID nodeID, final ObjectID objectID) {
    // to be consistent with createPrunedChangesAndAddObjectIDTo, return false
    return false;
  }

  public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> rescueIds) {
    return rescueIds;
  }

  public int getReferenceCount(final NodeID node) {
    return 0;
  }

  public void stop() {
    // TODO Auto-generated method stub

  }

  public void removeReferencedFrom(final NodeID nodeID, final Set<ObjectID> secondPass) {
    throw new ImplementMe();

  }

  public Set<ObjectID> addReferences(final NodeID nodeID, final Set<ObjectID> oids) {
    for (final ObjectID oid : oids) {
      this.addReferenceCalls.add(new AddReferenceContext(nodeID, oid));
    }
    return oids;
  }

  public boolean startupNode(final NodeID nodeID) {
    return true;
  }

  public Set<NodeID> getConnectedClientIDs() {
    throw new ImplementMe();
  }

  public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    throw new ImplementMe();

  }

  public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    throw new ImplementMe();

  }

  public void addPrefetchedObjectIDs(NodeID nodeId, ObjectIDSet prefetchedIds) {
    throw new ImplementMe();

  }

  public void missingObjectIDs(NodeID clientID, ObjectIDSet missingObjectIDs) {
    throw new ImplementMe();

  }

}