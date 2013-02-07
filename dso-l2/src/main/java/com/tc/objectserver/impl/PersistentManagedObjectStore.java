/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PersistentManagedObjectStore {

  private final ManagedObjectPersistor objectPersistor;
  private volatile boolean             inShutdown;

  public PersistentManagedObjectStore(final ManagedObjectPersistor persistor) {
    this.objectPersistor = persistor;
  }

  public int getObjectCount() {
    return this.objectPersistor.getObjectCount();
  }

  public void addNewRoot(final Transaction tx, final String rootName, final ObjectID id) {
    this.objectPersistor.addRoot(tx, rootName, id);
  }

  public Set getRoots() {
    return this.objectPersistor.loadRoots();
  }

  public Set getRootNames() {
    return this.objectPersistor.loadRootNames();
  }

  public ObjectID getRootID(final String name) {
    return this.objectPersistor.loadRootID(name);
  }

  public Map getRootNamesToIDsMap() {
    return this.objectPersistor.loadRootNamesToIDs();
  }

  public boolean containsObject(final ObjectID id) {
    assertNotInShutdown();
    return this.objectPersistor.containsObject(id);
  }

  public ManagedObject createObject(final ObjectID id) {
    return new ManagedObjectImpl(id, objectPersistor);
  }

  public void commitObject(final Transaction tx, final ManagedObject managed) {
    assertNotInShutdown();
    this.objectPersistor.saveObject(tx, managed);
  }

  public void commitAllObjects(final Transaction tx, final Collection managed) {
    assertNotInShutdown();
    this.objectPersistor.saveAllObjects(tx, managed);
  }

  /**
   * Remove all objects by objectID
   *
   * @param toDelete objects to delete
   */
  public void removeAllObjectsByID(final Set<ObjectID> toDelete) {
    assertNotInShutdown();
    this.objectPersistor.deleteAllObjects(toDelete);
  }

  public ObjectIDSet getAllObjectIDs() {
    assertNotInShutdown();
    return this.objectPersistor.snapshotObjectIDs();
  }

  public ObjectIDSet getAllEvictableObjectIDs() {
    assertNotInShutdown();
    return this.objectPersistor.snapshotEvictableObjectIDs();
  }

  public boolean hasNoReferences(ObjectID id) {
    assertNotInShutdown();
    return this.objectPersistor.hasNoReferences(id);
  }

  public ManagedObject getObjectByID(final ObjectID id) {
    assertNotInShutdown();

    final ManagedObject rv = this.objectPersistor.loadObjectByID(id);
    if (rv == null) { return rv; }
    if (rv.isDirty()) { throw new AssertionError("Object loaded from persistor is dirty.  Persistor: "
                                                 + this.objectPersistor + ", ManagedObject: " + rv); }
    return rv;
  }

  public void shutdown() {
    assertNotInShutdown();
    this.inShutdown = true;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out = out.println(getClass().getName()).duplicateAndIndent();
    out.println("object count: " + this.objectPersistor.getObjectCount()).duplicateAndIndent();
    return out;
  }

  private void assertNotInShutdown() {
    if (this.inShutdown) { throw new ShutdownError(); }
  }
}
