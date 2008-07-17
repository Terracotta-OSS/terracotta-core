/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author steve
 */
public interface ClientStateManager extends PrettyPrintable {

  public void stop();

  /**
   * Initializes the internal datastructures for newly connected client
   */
  public void startupNode(NodeID nodeID);
  
  /**
   * Clears internal data structures for disconnected clients
   *
   */
  public void shutdownNode(NodeID deadNode);

  /**
   * The the server representation of the client's state now knows that clientID has a reference to objectID
   */
  public void addReference(NodeID nodeID, ObjectID objectID);

  /**
   * For the local state of the l1 named clientID remove all the objectIDs that are references
   */
  public void removeReferences(NodeID nodeID, Set<ObjectID> removed);

  public boolean hasReference(NodeID nodeID, ObjectID objectID);

  /**
   * Prunes the changes list down to include only changes for objects the given client has.
   * @param objectIDs TODO
   */
  public List<DNA> createPrunedChangesAndAddObjectIDTo(Collection<DNA> changes, BackReferences references, NodeID clientID, Set<ObjectID> objectIDs);
  
  public void addAllReferencedIdsTo(Set<ObjectID> rescueIds);

  public void removeReferencedFrom(NodeID nodeID, Set<ObjectID> secondPass);

  public Set<ObjectID> addReferences(NodeID nodeID, Set<ObjectID> oids);

  public int getReferenceCount(NodeID nodeID);

  public Set<NodeID> getConnectedClientIDs();
}
