/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public class ObjectManagerLookupResultsImpl implements ObjectManagerLookupResults {

  private final Map<ObjectID, ManagedObject> objects;
  private final ObjectIDSet                  lookupPendingObjectIDs;
  private final ObjectIDSet                  missingObjectIDs;

  public ObjectManagerLookupResultsImpl(Map<ObjectID, ManagedObject> objects, ObjectIDSet lookupPendingObjectIDs,
                                        ObjectIDSet missingObjectIDs) {
    this.objects = objects;
    this.lookupPendingObjectIDs = lookupPendingObjectIDs;
    this.missingObjectIDs = missingObjectIDs;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    return this.objects;
  }

  public ObjectIDSet getLookupPendingObjectIDs() {
    return lookupPendingObjectIDs;
  }

  public ObjectIDSet getMissingObjectIDs() {
    return missingObjectIDs;
  }
}
