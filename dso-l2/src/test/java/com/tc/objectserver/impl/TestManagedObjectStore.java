/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestManagedObjectStore implements ManagedObjectStore {

  public boolean       addNewWasCalled = false;
  public boolean       containsKey;
  public ObjectIDSet   keySet;
  public ManagedObject managedObject;
  private int          count;

  public boolean containsObject(final ObjectID id) {
    return this.containsKey;
  }

  public void addNewObject(final ManagedObject managed) {
    this.addNewWasCalled = true;
    this.count++;
  }

  public ObjectIDSet getAllObjectIDs() {
    return this.keySet;
  }

  public int getObjectCount() {
    return this.count;
  }

  public ManagedObject getObjectByID(final ObjectID id) {
    return this.managedObject;
  }

  public void commitObject(final PersistenceTransaction tx, final ManagedObject object) {
    return;
  }

  public void commitAllObjects(final PersistenceTransaction tx, final Collection c) {
    return;
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  public void removeAllObjectsByIDNow(final SortedSet<ObjectID> objectIds) {
    this.count -= objectIds.size();
    return;
  }

  public void removeAllObjectsByID(final GCResultContext gcResult) {
    removeAllObjectsByIDNow(new TreeSet(gcResult.getGCedObjectIDs()));
  }

  public void shutdown() {
    return;
  }

  public boolean inShutdown() {
    return false;
  }

  public ObjectID getRootID(final String name) {
    return null;
  }

  public Set getRoots() {
    return null;
  }

  public Set getRootNames() {
    return null;
  }

  public void addNewRoot(final PersistenceTransaction tx, final String rootName, final ObjectID id) {
    return;
  }

  public long nextObjectIDBatch(final int batchSize) {
    throw new ImplementMe();
  }

  public void setNextAvailableObjectID(final long startID) {
    throw new ImplementMe();
  }

  public Map getRootNamesToIDsMap() {
    return null;
  }

  public long currentObjectIDValue() {
    throw new ImplementMe();
  }

  public ObjectIDSet getAllEvictableObjectIDs() {
    throw new ImplementMe();
  }

  public ObjectIDSet getAllMapTypeObjectIDs() {
    throw new ImplementMe();
  }

}