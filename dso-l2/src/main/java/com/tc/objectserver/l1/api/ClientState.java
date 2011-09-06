/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;

import java.util.Set;

public interface ClientState {

  public void addReference(ObjectID id);

  public boolean containsReference(ObjectID id);

  public void removeReferences(Set<ObjectID> references);

  public void addReferencedIdsTo(Set<ObjectID> ids);

  public NodeID getNodeID();

  public Set<ObjectID> getReferences();

}