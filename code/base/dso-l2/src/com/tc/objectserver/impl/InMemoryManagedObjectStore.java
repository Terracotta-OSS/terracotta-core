/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.SyncObjectIdSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class InMemoryManagedObjectStore implements ManagedObjectStore {

  private long      objectIDSequence = 1000;
  private final Map roots            = new HashMap();
  private final Map managed;
  private boolean   inShutdown       = false;

  public InMemoryManagedObjectStore(Map managed) {
    this.managed = managed;
  }

  public synchronized boolean containsObject(ObjectID id) {
    assertNotInShutdown();
    return this.managed.containsKey(id);
  }

  public synchronized void addNewObject(ManagedObject managedObject) {
    assertNotInShutdown();
    localPut(managedObject);
  }

  private void localPut(ManagedObject managedObject) {
    this.managed.put(managedObject.getID(), managedObject);
  }

  public synchronized void commitObject(PersistenceTransaction tx, ManagedObject managedObject) {
    assertNotInShutdown();
    assertContains(managedObject);
  }

  public synchronized void commitAllObjects(PersistenceTransaction tx, Collection managedObjects) {
    assertNotInShutdown();
    for (Iterator i = managedObjects.iterator(); i.hasNext();) {
      assertContains((ManagedObject) i.next());
    }
  }

  private void removeObjectByID(PersistenceTransaction tx, ObjectID id) {
    this.managed.remove(id);
  }

  public synchronized void removeAllObjectsByID(PersistenceTransaction tx, Collection ids) {
    assertNotInShutdown();
    for (Iterator i = ids.iterator(); i.hasNext();) {
      removeObjectByID(tx, (ObjectID) i.next());
    }
  }

  public void removeAllObjectsByIDNow(PersistenceTransaction tx, Collection objectIds) {
    removeAllObjectsByID(tx, objectIds);
  }

  public synchronized SyncObjectIdSet getAllObjectIDs() {
    assertNotInShutdown();
    SyncObjectIdSet rv = new SyncObjectIdSet();
    rv.addAll(managed.keySet());
    return rv;
  }

  public synchronized int getObjectCount() {
    return managed.size();
  }

  public synchronized ManagedObject getObjectByID(ObjectID id) {
    assertNotInShutdown();
    return (ManagedObject) this.managed.get(id);
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName()).duplicateAndIndent().print("managed: ").visit(managed).println();
    return out;
  }

  public synchronized void shutdown() {
    assertNotInShutdown();
    this.inShutdown = true;
  }

  public synchronized boolean inShutdown() {
    return this.inShutdown;
  }

  private synchronized void assertNotInShutdown() {
    if (inShutdown) throw new ShutdownError();
  }

  private synchronized void assertContains(ManagedObject managedObject) {
    if (!containsObject(managedObject.getID())) throw new AssertionError("Object store does not contain "
                                                                         + managedObject);
  }

  public ObjectID getRootID(String name) {
    return (ObjectID) (roots.containsKey(name) ? roots.get(name) : ObjectID.NULL_ID);
  }

  public Set getRoots() {
    return new HashSet(roots.values());
  }

  public Set getRootNames() {
    return roots.keySet();
  }

  public void addNewRoot(PersistenceTransaction tx, String rootName, ObjectID id) {
    roots.put(rootName, id);
  }

  public synchronized long nextObjectIDBatch(int batchSize) {
    long rv = objectIDSequence;
    objectIDSequence += batchSize;
    return rv;
  }

  public void setTransientData(ManagedObjectStateFactory stateFactory) {
    assertNotInShutdown();
  }

}
