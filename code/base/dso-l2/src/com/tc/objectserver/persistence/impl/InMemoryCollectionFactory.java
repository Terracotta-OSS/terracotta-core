/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
