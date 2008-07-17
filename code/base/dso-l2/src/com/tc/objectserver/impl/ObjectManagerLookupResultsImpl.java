/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.object.ObjectID;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ObjectManagerLookupResultsImpl implements ObjectManagerLookupResults {

  private final Map<ObjectID, ManagedObject>  objects;
  private final Set<ObjectID>                 lookupPendingObjectIDs;

  public ObjectManagerLookupResultsImpl(Map<ObjectID, ManagedObject> objects ) {
    this(objects, Collections.<ObjectID>emptySet());
  }
  
  public ObjectManagerLookupResultsImpl(Map<ObjectID, ManagedObject> objects, Set<ObjectID> lookupPendingObjectIDs) {
    this.objects = objects;
    this.lookupPendingObjectIDs = lookupPendingObjectIDs;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    return this.objects;
  }

  public Set<ObjectID> getLookupPendingObjectIDs() {
    return lookupPendingObjectIDs;
  }
}
