/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import java.util.Map;
import java.util.Set;

public class SleepycatCollectionFactory implements PersistentCollectionFactory {

  public Map createPersistentMap(ObjectID id) {
    return new SleepycatPersistableMap(id);
  }

  public Set createPersistentSet(ObjectID id) {
    return new SleepycatPersistableSet(id);
  }

}
