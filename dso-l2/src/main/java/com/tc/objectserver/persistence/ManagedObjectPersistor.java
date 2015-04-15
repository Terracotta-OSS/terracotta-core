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
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.sequence.ObjectIDSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author tim
 */
public class ManagedObjectPersistor  {

  private static final String ROOT_DB = "root_db";
  private static final String OBJECT_ID_SEQUENCE = "object_id_sequence";


  private final KeyValueStorage<String, ObjectID> rootMap;
  private final KeyValueStorage<ObjectID, ManagedObject> objectMap;
  private final ObjectIDSequence objectIDSequence;

  private final ObjectIDSetMaintainer oidSetMaintainer;

  public ManagedObjectPersistor(StorageManager storageManager, SequenceManager sequenceManager, final ObjectIDSetMaintainer oidSetMaintainer) {
    this.rootMap = storageManager.getKeyValueStorage(ROOT_DB, String.class, ObjectID.class);
    this.oidSetMaintainer = oidSetMaintainer;
    this.objectMap = new ObjectMap(this, storageManager);
    this.objectIDSequence = new ObjectIDSequenceImpl(sequenceManager.getSequence(OBJECT_ID_SEQUENCE));
  }

  public static void addConfigsTo(final Map<String, KeyValueStorageConfig<?, ?>> configs, final ObjectIDSetMaintainer objectIDSetMaintainer,
                                  final StorageManagerFactory storageManagerFactory) {
    configs.put(ROOT_DB, ImmutableKeyValueStorageConfig.builder(String.class, ObjectID.class)
        .valueTransformer(ObjectIDTransformer.INSTANCE)
        .build());
    ObjectMap.addConfigTo(configs, objectIDSetMaintainer, storageManagerFactory);
  }

  public void close() {
    //
  }

  public Set loadRoots() {
    return new HashSet<ObjectID>(rootMap.values());
  }

  public Set loadRootNames() {
    return rootMap.keySet();
  }

  public ObjectID loadRootID(String name) {
    ObjectID id = rootMap.get(name);
    return id == null ? ObjectID.NULL_ID : id;
  }

  public void addRoot(Transaction tx, String name, ObjectID id) {
    rootMap.put(name, id);
  }

  public ManagedObject loadObjectByID(ObjectID id) {
    return objectMap.get(id);
  }

  public void saveObject(Transaction tx, ManagedObject managedObject) {
    objectMap.put(managedObject.getID(), managedObject, managedObject.getManagedObjectState().getType());
    managedObject.setIsDirty(false);
  }

  public void saveAllObjects(Transaction tx, Collection<ManagedObject> managed) {
    for (ManagedObject managedObject : managed) {
      saveObject(tx, managedObject);
    }
  }

  public void deleteAllObjects(Set<ObjectID> ids) {
    objectMap.removeAll(ids);
  }

  public Map<String, ObjectID> loadRootNamesToIDs() {
    return asJdkMap(rootMap);
  }

  private <K, V> Map<K, V> asJdkMap(KeyValueStorage<K, V> map) {
    Map<K, V> m = new HashMap<K, V>();
    for (K k : map.keySet()) {
      m.put(k, map.get(k));
    }
    return m;
  }

  public int getObjectCount() {
    return (int) objectMap.size();
  }

  public boolean containsObject(ObjectID id) {
    return objectMap.containsKey(id);
  }

  public ObjectIDSet snapshotObjectIDs() {
    return oidSetMaintainer.objectIDSnapshot();
  }

  public ObjectIDSet snapshotEvictableObjectIDs() {
    return oidSetMaintainer.evictableObjectIDSetSnapshot();
  }

  public ObjectIDSequence getObjectIDSequence() {
    return objectIDSequence;
  }

  public boolean hasNoReferences(ObjectID id) {
    return oidSetMaintainer.hasNoReferences(id);
  }
}
