/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.storage.api.TCMapsDatabase.BackingMapFactory;

import java.util.HashMap;
import java.util.Map;

public class HashMapBackingMapFactory implements BackingMapFactory {

  public Map createBackingMapFor(final ObjectID mapID) {
    return new HashMap();
  }

}
