/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;

import java.util.Map;

public interface PersistentCollectionFactory {
  
  public Map createPersistentMap(ObjectID id);

}
