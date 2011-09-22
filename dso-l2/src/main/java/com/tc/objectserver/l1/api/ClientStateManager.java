/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.invalidation.Invalidations;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Client State Manager Interface
 */
public interface ClientStateManager {

  /**
   * Initializes the internal data structures for newly connected client
   * 
   * @return boolean true if this newly connecting client can be accepted, else false
   */
  public boolean startupNode(NodeID nodeID);

  /**
   * Clears internal data structures for disconnected clients
   */
  public void shutdownNode(NodeID deadNode);

  /**
   * The the server representation of the client's state now knows that clientID has a reference to objectID
   */
  public void addReference(NodeID nodeID, ObjectID objectID);

  /**
   * From the local state of the l1 named nodeID remove all the objectIDs that are references and also remove from the
   * requested list any refrence already present
   * 
   * @param nodeID nodeID of the client requesting the objects
   * @param removed set of objects removed from the client
   * @param requested set of Objects requested, this set is mutated to remove any object that is already present in the
   *        client.
   */
  public void removeReferences(NodeID nodeID, Set<ObjectID> removed, Set<ObjectID> requested);

  public boolean hasReference(NodeID nodeID, ObjectID objectID);

  /**
   * Prunes the changes list down to include only changes for objects the given client has.
   */
  public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, ApplyTransactionInfo references,
                                                       NodeID clientID, Set<ObjectID> objectIDs,
                                                       Invalidations invalidationsForClient);

  public Set<ObjectID> addAllReferencedIdsTo(Set<ObjectID> rescueIds);

  public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass);

  public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids);

  public int getReferenceCount(NodeID nodeID);

  public Set<NodeID> getConnectedClientIDs();

  public void registerObjectReferenceAddListener(ObjectReferenceAddListener listener);

  public void unregisterObjectReferenceAddListener(ObjectReferenceAddListener listener);

  /**
   * Add prefetched ObjectIDs for the client
   */
  public void addPrefetchedObjectIDs(NodeID nodeId, ObjectIDSet prefetchedIds);

  public void missingObjectIDs(NodeID clientID, ObjectIDSet missingObjectIDs);

}
