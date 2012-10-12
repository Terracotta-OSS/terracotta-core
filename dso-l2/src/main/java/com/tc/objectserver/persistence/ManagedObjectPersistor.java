package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.sequence.MutableSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * @author tim
 */
public class ManagedObjectPersistor {

  // This should be persistent
  private final KeyValueStorage<String, ObjectID> rootMap;
  private final KeyValueStorage<ObjectID, ManagedObject> objectMap;
  private final MutableSequence objectIDSequence;

  private final ObjectIDSetMaintainer oidSetMaintainer;

  public ManagedObjectPersistor(KeyValueStorage<String, ObjectID> rootMap, KeyValueStorage<Long, byte[]> objectMap, MutableSequence objectIDSequence, final ObjectIDSetMaintainer oidSetMaintainer) {
    this.rootMap = rootMap;
    this.oidSetMaintainer = oidSetMaintainer;
    this.objectMap = new ObjectMap(this, objectMap);
    this.objectIDSequence = objectIDSequence;
  }

  public static KeyValueStorageConfig<String, ObjectID> rootMapConfig() {
    KeyValueStorageConfig<String, ObjectID> config = new KeyValueStorageConfigImpl<String, ObjectID>(String.class, ObjectID.class);
    config.setKeySerializer(StringSerializer.INSTANCE);
    config.setValueSerializer(ObjectIDSerializer.INSTANCE);
    return config;
  }

  public static KeyValueStorageConfig<Long, byte[]> objectConfig() {
    return ObjectMap.getConfig();
  }

  public void close() {
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

  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  public long currentObjectIDValue() {
    return objectIDSequence.current();
  }

  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  public void saveObject(Transaction tx, ManagedObject managedObject) {
    objectMap.put(managedObject.getID(), managedObject);
    managedObject.setIsDirty(false);
  }

  public void saveAllObjects(Transaction tx, Collection managed) {
    for (ManagedObject managedObject : (Collection<ManagedObject> ) managed) {
      saveObject(tx, managedObject);
    }
  }

  public void deleteAllObjects(SortedSet<ObjectID> ids) {
    objectMap.removeAll(ids);
  }

  public Map loadRootNamesToIDs() {
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

}
