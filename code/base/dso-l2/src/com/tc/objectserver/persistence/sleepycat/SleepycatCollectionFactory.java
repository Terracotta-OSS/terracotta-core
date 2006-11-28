/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import java.util.Map;

public class SleepycatCollectionFactory implements PersistentCollectionFactory {

  public Map createPersistentMap(ObjectID id) {
    return new SleepycatPersistableMap(id);
  }

}
