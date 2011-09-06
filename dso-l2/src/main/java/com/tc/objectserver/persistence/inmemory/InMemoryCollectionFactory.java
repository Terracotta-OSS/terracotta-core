/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.util.Map;
import java.util.Set;

public class InMemoryCollectionFactory implements PersistentCollectionFactory {

  public Map createPersistentMap(ObjectID id) {
    return new THashMap(0);
  }

  public Set createPersistentSet(ObjectID id) {
    return new THashSet(0);
  }

}
