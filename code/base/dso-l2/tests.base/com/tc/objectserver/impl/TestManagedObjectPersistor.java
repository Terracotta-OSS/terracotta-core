/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.SyncObjectIdSet;
import com.tc.util.SyncObjectIdSetImpl;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TestManagedObjectPersistor implements ManagedObjectPersistor {

  public final NoExceptionLinkedQueue loadByObjectIDCalls = new NoExceptionLinkedQueue();
  public final Map                    map;
  public boolean                      closeCalled         = false;
  public SyncObjectIdSet              allObjectIDs        = new SyncObjectIdSetImpl();

  public TestManagedObjectPersistor(Map map) {
    this.map = map;
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

  public void prettyPrint(PrettyPrinter out) {
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

  public void deleteAllObjectsByID(PersistenceTransaction tx, Collection ids) {
    for (Iterator i = ids.iterator(); i.hasNext();) {
      deleteObjectByID(tx, (ObjectID) i.next());
    }
  }

  public Map loadRootNamesToIDs() {
    return null;
  }

}
