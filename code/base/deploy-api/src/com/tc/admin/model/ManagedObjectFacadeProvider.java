/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public interface ManagedObjectFacadeProvider {
  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;
}
