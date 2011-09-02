/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.ObjectID;

import java.util.Set;

public interface NodesWithObjectsMessage extends ClusterMetaDataMessage {

  public Set<ObjectID> getObjectIDs();

  public void addObjectID(ObjectID objectID);

}
