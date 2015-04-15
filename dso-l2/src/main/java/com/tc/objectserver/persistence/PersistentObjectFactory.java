/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.object.ObjectID;

/**
 * @author tim
 */
public class PersistentObjectFactory {
  private static final KeyValueStorageConfig<Object, Object> MAP_CONFIG = ImmutableKeyValueStorageConfig.builder(Object.class, Object.class)
      .keyTransformer(LiteralSerializer.INSTANCE)
      .valueTransformer(LiteralSerializer.INSTANCE)
      .concurrency(1).build();

  private final StorageManager storageManager;
  private final KeyValueStorageConfig<Object, Object> defaultConfig;

  public PersistentObjectFactory(final StorageManager storageManager, final StorageManagerFactory storageManagerFactory) {
    this.storageManager = storageManager;
    defaultConfig = storageManagerFactory.wrapMapConfig(MAP_CONFIG);
  }

  public synchronized KeyValueStorage<Object, Object> getKeyValueStorage(ObjectID objectID, final boolean create) throws ObjectNotFoundException {
    KeyValueStorage<Object, Object> map = storageManager.getKeyValueStorage(objectID.toString(), Object.class, Object.class);
    if (map == null) {
      if (create) {
        map = storageManager.createKeyValueStorage(objectID.toString(), defaultConfig);
      } else {
        throw new ObjectNotFoundException("Map for object id " + objectID + " not found.");
      }
    }
    return map;
  }

  public synchronized void destroyKeyValueStorage(ObjectID oid) {
    storageManager.destroyKeyValueStorage(oid.toString());
  }
}
