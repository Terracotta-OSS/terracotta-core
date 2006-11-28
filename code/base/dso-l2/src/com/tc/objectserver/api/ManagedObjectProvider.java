/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
