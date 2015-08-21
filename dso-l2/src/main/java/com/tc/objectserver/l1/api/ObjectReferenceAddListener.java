/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.object.ObjectID;

import java.util.Set;

public interface ObjectReferenceAddListener {

  public void objectReferenceAdded(ObjectID objectID);

  public void objectReferencesAdded(Set<ObjectID> objectIDs);

}
