package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;

import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;
/**
 * @author tim
 */
public class PersistentObjectFactory {

  private final IPersistentStorage storageManager;

  public PersistentObjectFactory(IPersistentStorage storageManager) {
    this.storageManager = storageManager;
  }

  public synchronized KeyValueStorage<Object, Object> getKeyValueStorage(ObjectID objectID, boolean create) throws ObjectNotFoundException {
    return storageManager.getKeyValueStorage(objectID.toString(), Object.class, Object.class);
  }

  public synchronized void destroyKeyValueStorage(ObjectID oid) {
    storageManager.destroyKeyValueStorage(oid.toString());
  }
}
