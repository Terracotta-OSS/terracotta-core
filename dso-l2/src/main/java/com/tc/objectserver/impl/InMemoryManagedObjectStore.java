/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class InMemoryManagedObjectStore implements ManagedObjectStore {

  private long              objectIDSequence = 1000;
  private final Map         roots            = new HashMap();
  private final Map         managed;
  private final ObjectIDSet evictables       = new ObjectIDSet();
  private boolean           inShutdown       = false;

  public InMemoryManagedObjectStore(final Map managed) {
    this.managed = managed;
  }

  public synchronized boolean containsObject(final ObjectID id) {
    assertNotInShutdown();
    return this.managed.containsKey(id);
  }

  public synchronized void addNewObject(final ManagedObject managedObject) {
    assertNotInShutdown();
    localPut(managedObject);
  }

  private void localPut(final ManagedObject managedObject) {
    this.managed.put(managedObject.getID(), managedObject);
    if (PersistentCollectionsUtil.isEvictableMapType(managedObject.getManagedObjectState().getType())) {
      this.evictables.add(managedObject.getID());
    }
  }

  public synchronized void commitObject(final PersistenceTransaction tx, final ManagedObject managedObject) {
    assertNotInShutdown();
    // Nothing to do here since its in memory DB
  }

  public synchronized void commitAllObjects(final PersistenceTransaction tx, final Collection managedObjects) {
    assertNotInShutdown();
    // Nothing to do here since its in memory DB
  }

  private void removeObjectByID(final ObjectID id) {
    this.managed.remove(id);
    this.evictables.remove(id);
  }

  public synchronized void removeAllObjectsByIDNow(final SortedSet<ObjectID> objectIds) {
    assertNotInShutdown();
    for (final ObjectID element : objectIds) {
      removeObjectByID(element);
    }
  }

  public void removeAllObjectsByID(final DGCResultContext dgcResultContext) {
    removeAllObjectsByIDNow(dgcResultContext.getGarbageIDs());
  }

  public synchronized ObjectIDSet getAllObjectIDs() {
    assertNotInShutdown();
    return new ObjectIDSet(this.managed.keySet());
  }

  public synchronized int getObjectCount() {
    return this.managed.size();
  }

  public synchronized ManagedObject getObjectByID(final ObjectID id) {
    assertNotInShutdown();
    return (ManagedObject) this.managed.get(id);
  }

  public synchronized PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.println(getClass().getName()).duplicateAndIndent().print("managed: ").visit(this.managed).println();
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
    if (this.inShutdown) { throw new ShutdownError(); }
  }

  // private synchronized void assertContains(ManagedObject managedObject) {
  // if (!containsObject(managedObject.getID())) throw new AssertionError("Object store does not contain "
  // + managedObject);
  // }

  public ObjectID getRootID(final String name) {
    return (ObjectID) (this.roots.containsKey(name) ? this.roots.get(name) : ObjectID.NULL_ID);
  }

  public Set getRoots() {
    return new HashSet(this.roots.values());
  }

  public Set getRootNames() {
    return this.roots.keySet();
  }

  public void addNewRoot(final PersistenceTransaction tx, final String rootName, final ObjectID id) {
    this.roots.put(rootName, id);
  }

  public synchronized long nextObjectIDBatch(final int batchSize) {
    final long rv = this.objectIDSequence;
    this.objectIDSequence += batchSize;
    return rv;
  }

  public void setNextAvailableObjectID(final long startID) {
    Assert.assertTrue(startID >= this.objectIDSequence);
    this.objectIDSequence = startID;
  }

  public void setTransientData(final ManagedObjectStateFactory stateFactory) {
    assertNotInShutdown();
  }

  public Map getRootNamesToIDsMap() {
    return this.roots;
  }

  public long currentObjectIDValue() {
    return this.objectIDSequence;
  }

  public ObjectIDSet getAllEvictableObjectIDs() {
    return new ObjectIDSet(this.evictables);
  }

  public ObjectIDSet getAllMapTypeObjectIDs() {
    throw new UnsupportedOperationException();
  }
}
