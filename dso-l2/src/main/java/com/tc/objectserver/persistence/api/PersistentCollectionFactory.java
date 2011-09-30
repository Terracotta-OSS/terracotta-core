/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;

import java.util.Map;
import java.util.Set;

public interface PersistentCollectionFactory {

  public Map createPersistentMap(ObjectID id);

  public Set createPersistentSet(ObjectID id);

}
