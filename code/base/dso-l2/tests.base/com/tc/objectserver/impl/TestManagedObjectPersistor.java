/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.NullSyncObjectIdSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class TestManagedObjectPersistor implements ManagedObjectPersistor {

  public final NoExceptionLinkedQueue loadByObjectIDCalls = new NoExceptionLinkedQueue();
  public final Map                    map;
  public boolean                      closeCalled         = false;
  public SyncObjectIdSet              allObjectIDs        = new SyncObjectIdSetImpl();
  private final SyncObjectIdSet       extantObjectIDs;

  public TestManagedObjectPersistor(Map map) {
    this.map = map;

    this.extantObjectIDs = getAllObjectIDs();
  }

  public int getObjectCount() {
    return extantObjectIDs.size();
  }

  public boolean addNewObject(ObjectID id) {
    return extantObjectIDs.add(id);
  }

  public boolean containsObject(ObjectID id) {
    return extantObjectIDs.contains(id);
  }

  public void removeAllObjectsByID(SortedSet<ObjectID> ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public ObjectIDSet snapshotObjects() {
    return this.extantObjectIDs.snapshot();
  }

  public ManagedObject loadObjectByID(ObjectID id) {
    loadByObjectIDCalls.put(id);
    return (ManagedObject) map.get(id);
  }

  public ObjectID loadRootID(String name) {
    return null;
  }

  public void addRoot(PersistenceTransaction tx, String name, ObjectID id) {
    return;
  }

  public void saveObject(PersistenceTransaction tx, ManagedObject managedObject) {
    map.put(managedObject.getID(), managedObject);
  }

  public void saveAllObjects(PersistenceTransaction tx, Collection managed) {
    for (Iterator i = managed.iterator(); i.hasNext();) {
      saveObject(tx, (ManagedObject) i.next());
    }
  }

  public void deleteObjectByID(PersistenceTransaction tx, ObjectID id) {
    map.remove(id);
  }

  public void prettyPrint(PrettyPrinterImpl out) {
    return;
  }

  public Set loadRoots() {
    return null;
  }

  public Set loadRootNames() {
    return null;
  }

  public long nextObjectIDBatch(int batchSize) {
    throw new ImplementMe();
  }

  public void setNextAvailableObjectID(long startID) {
    throw new ImplementMe();
  }

  public SyncObjectIdSet getAllObjectIDs() {
    return allObjectIDs;
  }

  public SyncObjectIdSet getAllMapsObjectIDs() {
    return new NullSyncObjectIdSet();
  }

  public void deleteAllObjectsByID(PersistenceTransaction tx, SortedSet<ObjectID> ids) {
    for (Iterator i = ids.iterator(); i.hasNext();) {
      deleteObjectByID(tx, (ObjectID) i.next());
    }
  }

  public Map loadRootNamesToIDs() {
    return null;
  }

  public boolean addMapTypeObject(ObjectID id) {
    return false;
  }

  public boolean containsMapType(ObjectID id) {
    return false;
  }

  public void removeAllMapTypeObject(Collection ids) {
    return;
  }

}
