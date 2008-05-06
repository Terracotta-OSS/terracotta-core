/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.text.PrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TestClientStateManager implements ClientStateManager {

  public NodeID     shutdownClient    = null;
  public Collection allClientIDs      = new HashSet();
  public List       addReferenceCalls = new ArrayList();

  public void shutdownNode(NodeID deadNode) {
    this.shutdownClient = deadNode;
  }

  public void addReference(NodeID nodeID, ObjectID objectID) {
    addReferenceCalls.add(new AddReferenceContext(nodeID, objectID));
  }

  public static class AddReferenceContext {
    public final NodeID   nodeID;
    public final ObjectID objectID;

    private AddReferenceContext(NodeID nodeID, ObjectID objectID) {
      this.nodeID = nodeID;
      this.objectID = objectID;
    }
  }

  public void removeReferences(NodeID nodeID, Set removed) {
    //
  }

  public List createPrunedChangesAndAddObjectIDTo(Collection changes, BackReferences includeIDs, NodeID clientID,
                                                  Set objectIDs) {
    return Collections.EMPTY_LIST;
  }

  public boolean hasReference(NodeID nodeID, ObjectID objectID) {
    // to be consistent with createPrunedChangesAndAddObjectIDTo, return false
    return false;
  }

  public void addAllReferencedIdsTo(Set rescueIds) {
    throw new ImplementMe();

  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  public Collection getAllClientIDs() {
    return allClientIDs;
  }

  public void stop() {
    // TODO Auto-generated method stub

  }

  public void removeReferencedFrom(NodeID nodeID, Set secondPass) {
    throw new ImplementMe();

  }

  public Set addReferences(NodeID nodeID, Set oids) {
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      addReferenceCalls.add(new AddReferenceContext(nodeID, oid));
    }
    return oids;
  }

  public void startupNode(NodeID nodeID) {
    // NOP
  }

}