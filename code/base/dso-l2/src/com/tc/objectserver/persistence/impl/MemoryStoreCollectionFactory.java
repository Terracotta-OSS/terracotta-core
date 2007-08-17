/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;

import java.util.Map;

public class MemoryStoreCollectionFactory implements PersistentCollectionFactory {
  private MemoryDataStoreClient           db;
  private MemoryStoreCollectionsPersistor persistor;

  public Map createPersistentMap(ObjectID id) {
    if (db == null || persistor == null) {
      throw new TCRuntimeException("Must set db and persistor");
    }
    return new MemoryStorePersistableMap(id, persistor, db);
  }
  
  public void setPersistor(MemoryStoreCollectionsPersistor persistor) {
    this.persistor = persistor;
  }
  
  public void setMemoryDataStore(MemoryDataStoreClient db) {
    this.db = db;
  }

}
