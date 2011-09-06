/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
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
  private final SyncObjectIdSet       extantObjectIDs;

  public TestManagedObjectPersistor(final Map map) {
    this.map = map;
    this.extantObjectIDs = new SyncObjectIdSetImpl();
  }

  public int getObjectCount() {
    return this.extantObjectIDs.size();
  }

  public boolean addNewObject(final ManagedObject managed) {
    return this.extantObjectIDs.add(managed.getID());
  }

  public boolean containsObject(final ObjectID id) {
    return this.extantObjectIDs.contains(id);
  }

  public void removeAllObjectIDs(final SortedSet<ObjectID> ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public ObjectIDSet snapshotObjectIDs() {
    return this.extantObjectIDs.snapshot();
  }

  public ObjectIDSet snapshotEvictableObjectIDs() {
    throw new ImplementMe();
  }

  public ManagedObject loadObjectByID(final ObjectID id) {
    this.loadByObjectIDCalls.put(id);
    return (ManagedObject) this.map.get(id);
  }

  public ObjectID loadRootID(final String name) {
    return null;
  }

  public void addRoot(final PersistenceTransaction tx, final String name, final ObjectID id) {
    return;
  }

  public void saveObject(final PersistenceTransaction tx, final ManagedObject managedObject) {
    this.map.put(managedObject.getID(), managedObject);
  }

  public void saveAllObjects(final PersistenceTransaction tx, final Collection managed) {
    for (final Iterator i = managed.iterator(); i.hasNext();) {
      saveObject(tx, (ManagedObject) i.next());
    }
  }

  public void deleteObjectByID(final ObjectID id) {
    this.map.remove(id);
  }

  public Set loadRoots() {
    return null;
  }

  public Set loadRootNames() {
    return null;
  }

  public long nextObjectIDBatch(final int batchSize) {
    throw new ImplementMe();
  }

  public void setNextAvailableObjectID(final long startID) {
    throw new ImplementMe();
  }

  public void deleteAllObjects(final SortedSet<ObjectID> sortedOids) {
    for (final Object element : sortedOids) {
      deleteObjectByID((ObjectID) element);
    }
    removeAllFromOtherExtantSets(sortedOids);
  }

  private void removeAllFromOtherExtantSets(final Collection ids) {
    this.extantObjectIDs.removeAll(ids);
  }

  public Map loadRootNamesToIDs() {
    return null;
  }

  public long currentObjectIDValue() {
    return -1;
  }

  public ObjectIDSet snapshotMapTypeObjectIDs() {
    throw new ImplementMe();
  }

  public void close() {
    // Nothing to close
  }
}
