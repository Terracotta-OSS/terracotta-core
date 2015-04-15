/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
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

  @Override
  public void shutdownNode(final NodeID deadNode) {
    this.shutdownClient = deadNode;
  }

  @Override
  public boolean addReference(final NodeID nodeID, final ObjectID objectID) {
    return this.addReferenceCalls.add(new AddReferenceContext(nodeID, objectID));
  }

  public static class AddReferenceContext {
    public final NodeID   nodeID;
    public final ObjectID objectID;

    private AddReferenceContext(final NodeID nodeID, final ObjectID objectID) {
      this.nodeID = nodeID;
      this.objectID = objectID;
    }
  }

  @Override
  public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested) {
    //
  }

  @Override
  public List<DNA> createPrunedChangesAndAddObjectIDTo(final Collection<DNA> changes,
                                                       final ApplyTransactionInfo includeIDs, final NodeID clientID,
                                                       final Set<ObjectID> objectIDs, Invalidations invalidIDs) {
    return Collections.emptyList();
  }

  @Override
  public boolean hasReference(final NodeID nodeID, final ObjectID objectID) {
    // to be consistent with createPrunedChangesAndAddObjectIDTo, return false
    return false;
  }

  @Override
  public Set<ObjectID> addAllReferencedIdsTo(final Set<ObjectID> rescueIds) {
    return rescueIds;
  }

  @Override
  public int getReferenceCount(final NodeID node) {
    return 0;
  }

  public void stop() {
    // TODO Auto-generated method stub

  }

  @Override
  public void removeReferencedFrom(final NodeID nodeID, final Set<ObjectID> secondPass) {
    throw new ImplementMe();

  }

  @Override
  public Set<ObjectID> addReferences(final NodeID nodeID, final Set<ObjectID> oids) {
    for (final ObjectID oid : oids) {
      this.addReferenceCalls.add(new AddReferenceContext(nodeID, oid));
    }
    return oids;
  }

  @Override
  public boolean startupNode(final NodeID nodeID) {
    return true;
  }

  @Override
  public Set<NodeID> getConnectedClientIDs() {
    throw new ImplementMe();
  }

  @Override
  public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    throw new ImplementMe();

  }

  @Override
  public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener) {
    throw new ImplementMe();

  }

}