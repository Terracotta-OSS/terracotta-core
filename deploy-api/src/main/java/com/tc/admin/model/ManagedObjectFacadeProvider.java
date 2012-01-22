/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public interface ManagedObjectFacadeProvider {
  ManagedObjectFacade lookupFacade(ObjectID objectID, int limit) throws NoSuchObjectException;
}
