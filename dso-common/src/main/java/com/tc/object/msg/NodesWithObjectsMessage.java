/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;

import java.util.Set;

public interface NodesWithObjectsMessage extends ClusterMetaDataMessage {

  public Set<ObjectID> getObjectIDs();

  public void addObjectID(ObjectID objectID);

}
