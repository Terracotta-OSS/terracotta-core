/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.objectserver.persistence.api.PersistentMapStore;

import java.util.Hashtable;
import java.util.Map;

public class InMemoryPersistentMapStore implements PersistentMapStore {

  Map map = new Hashtable();

  public String get(String key) {
    return (String) map.get(key);
  }

  public void put(String key, String value) {
    map.put(key, value);
  }

  public boolean remove(String key) {
    return (map.remove(key) != null);
  }

}
