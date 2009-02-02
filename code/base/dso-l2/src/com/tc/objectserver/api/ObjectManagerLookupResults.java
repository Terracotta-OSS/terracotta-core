/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public interface ObjectManagerLookupResults {

  Map<ObjectID, ManagedObject> getObjects();

  ObjectIDSet getLookupPendingObjectIDs();

  ObjectIDSet getMissingObjectIDs();

}
