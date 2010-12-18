/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;

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

  public void removeReferences(final NodeID nodeID, final Set<ObjectID> removed) {
    //
  }

  public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                       final ApplyTransactionInfo includeIDs, final NodeID clientID,
                                                       final Set<ObjectID> objectIDs, final Set<ObjectID> invalidIDs) {
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

  public void startupNode(final NodeID nodeID) {
    // NOP
  }

  public Set<NodeID> getConnectedClientIDs() {
    throw new ImplementMe();
  }
}