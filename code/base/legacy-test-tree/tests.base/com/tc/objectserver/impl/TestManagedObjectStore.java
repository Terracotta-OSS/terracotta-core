/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Set;

public class TestManagedObjectStore implements ManagedObjectStore {

  public boolean       addNewWasCalled = false;
  public boolean       containsKey;
  public Set           keySet;
  public ManagedObject managedObject;
  private int          count;

  public boolean containsObject(ObjectID id) {
    return containsKey;
  }

  public void addNewObject(ManagedObject managed) {
    addNewWasCalled = true;
    count++;
  }

  public Set getAllObjectIDs() {
    return keySet;
  }

  public int getObjectCount() {
    return count;
  }

  public ManagedObject getObjectByID(ObjectID id) {
    return managedObject;
  }

  public void commitObject(PersistenceTransaction tx, ManagedObject object) {
    return;
  }

  public void commitAllObjects(PersistenceTransaction tx, Collection c) {
    return;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print(getClass().getName());
  }

  public void removeAllObjectsByIDNow(PersistenceTransaction tx, Collection objectIds) {
    count -= objectIds.size();
    return;
  }

  public void shutdown() {
    return;
  }

  public boolean inShutdown() {
    return false;
  }

  public ObjectID getRootID(String name) {
    return null;
  }

  public Set getRoots() {
    return null;
  }

  public Set getRootNames() {
    return null;
  }

  public void addNewRoot(PersistenceTransaction tx, String rootName, ObjectID id) {
    return;
  }

  public long nextObjectIDBatch(int batchSize) {
    throw new RuntimeException("Implement me!");
  }

}