package com.tc.objectserver.persistence.gb;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.gb.gbapi.GBMap;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.ObjectIDSet;

import java.util.*;

/**
 * @author tim
 */
public class GBManagedObjectPersistor implements ManagedObjectPersistor {

  // This should be persistent
  private final GBMap<String, ObjectID> rootMap = null;
  private final GBMap<ObjectID, ManagedObject> objectMap = null;
  private final GBSequence objectIDSequence = null;

  private final GBObjectIDSetMaintainer oidSetMaintainer = new GBObjectIDSetMaintainer();


  public GBManagedObjectPersistor() {

  }

  @Override
  public void close() {
  }

  @Override
  public Set loadRoots() {
    return new HashSet(rootMap.values());
  }

  @Override
  public Set loadRootNames() {
    return rootMap.keySet();
  }

  @Override
  public ObjectID loadRootID(String name) {
    return rootMap.get(name);
  }

  @Override
  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    rootMap.put(name, id);
  }

  @Override
  public ManagedObject loadObjectByID(ObjectID id) {
    return objectMap.get(id);
  }

  @Override
  public long nextObjectIDBatch(int batchSize) {
    return objectIDSequence.nextBatch(batchSize);
  }

  @Override
  public long currentObjectIDValue() {
    return objectIDSequence.current();
  }

  @Override
  public void setNextAvailableObjectID(long startID) {
    objectIDSequence.setNext(startID);
  }

  @Override
  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject) {
    objectMap.put(managedObject.getID(), managedObject);
  }

  @Override
  public void saveAllObjects(PersistenceTransaction tx, Collection managed) {
    for (ManagedObject managedObject : (Collection<ManagedObject> ) managed) {
      saveObject(tx, managedObject);
    }
  }

  @Override
  public void deleteAllObjects(SortedSet<ObjectID> ids) {
    objectMap.removeAll(ids);
  }

  @Override
  public Map loadRootNamesToIDs() {
    return asJdkMap(rootMap);
  }

  private <K, V> Map<K, V> asJdkMap(GBMap<K, V> map) {
    return null;
  }

  @Override
  public int getObjectCount() {
    return (int) objectMap.size();
  }

  @Override
  @Deprecated
  public boolean addNewObject(ManagedObject managed) {
    // This populates the evictable, map type, extant object id sets. They're maintained via our
    // mutation listener.
    return true;
  }

  @Override
  public boolean containsObject(ObjectID id) {
    return objectMap.containsKey(id);
  }

  @Override
  @Deprecated
  public void removeAllObjectIDs(SortedSet<ObjectID> ids) {
    // Handled by the mutation listener like addNewObject();
  }

  @Override
  public ObjectIDSet snapshotObjectIDs() {
    return oidSetMaintainer.objectIDSnapshot();
  }

  @Override
  public ObjectIDSet snapshotEvictableObjectIDs() {
    return oidSetMaintainer.evictableObjectIDSetSnapshot();
  }

  @Override
  @Deprecated
  public ObjectIDSet snapshotMapTypeObjectIDs() {
    // Only appears to be used in tests, probably get rid of this.
    return oidSetMaintainer.mapObjectIDSetSnapshot();
  }
}
