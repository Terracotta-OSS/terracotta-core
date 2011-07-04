/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;

public interface ManagedObjectProvider {

  /**
   * @param id - an id to be requested. this is blocking for now. If it is checked out THIS WILL BLOCK
   * @return
   */
  public ManagedObject getObjectByID(ObjectID id);

}
