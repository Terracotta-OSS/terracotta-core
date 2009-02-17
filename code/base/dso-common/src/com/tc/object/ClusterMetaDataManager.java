/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.lockmanager.api.ThreadID;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ClusterMetaDataManager {

  public Set<NodeID> getNodesWithObject(ObjectID id);

  public Map<ObjectID, Set<NodeID>> getNodesWithObjects(Collection<ObjectID> ids);

  public Set<ObjectID> getKeysForOrphanedValues(ObjectID mapObjectID);

  public void setResponse(ThreadID threadId, Object response);

}