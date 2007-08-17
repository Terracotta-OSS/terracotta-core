/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.objectserver.persistence.api.PersistentMapStore;

public class MemoryStorePersistentMapStore implements PersistentMapStore {
  private MemoryDataStoreClient memoryStore;
  
  public MemoryStorePersistentMapStore(MemoryDataStoreClient memoryStore) {
    this.memoryStore = memoryStore;
  }
  
  public String get(String key) {
    if (key == null) { throw new NullPointerException(); }

    byte[] rtn = memoryStore.get(key.getBytes());
    if (rtn == null) return (null);
    return(rtn.toString());
  }

  public void put(String key, String value) {
    if (key == null || value == null) { throw new NullPointerException(); }

    memoryStore.put(key.getBytes(), value.getBytes());
  }
   
  public boolean remove(String key) {
    if (key == null) { throw new NullPointerException(); }

    // check existence before remove
    if (get(key) != null) {
      memoryStore.remove(key.getBytes());
      return (true);
    } else {
      return (false);
    }
  }

}
