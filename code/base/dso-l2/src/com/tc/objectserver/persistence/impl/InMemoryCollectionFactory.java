/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import gnu.trove.THashMap;

import java.util.Map;

public class InMemoryCollectionFactory implements PersistentCollectionFactory {

  public Map createPersistentMap(ObjectID id) {
    return new THashMap(0);
  }

}
