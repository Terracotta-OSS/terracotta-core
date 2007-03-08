/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet2;

import java.util.Collection;
import java.util.Set;

public class PersistentManagedObjectStore implements ManagedObjectStore {

  private final Set                    extantObjectIDs;
  private final ManagedObjectPersistor objectPersistor;
  private boolean                      inShutdown;

  public PersistentManagedObjectStore(ManagedObjectPersistor persistor) {
    this.objectPersistor = persistor;
    this.extantObjectIDs = objectPersistor.getAllObjectIDs();
  }

  public long nextObjectIDBatch(int batchSize) {
    long rv = this.objectPersistor.nextObjectIDBatch(batchSize);
    return rv;
  }

  public void addNewRoot(PersistenceTransaction tx, String rootName, ObjectID id) {
    objectPersistor.addRoot(tx, rootName, id);
  }

  public Set getRoots() {
    return objectPersistor.loadRoots();
  }

  public Set getRootNames() {
    return objectPersistor.loadRootNames();
  }

  public ObjectID getRootID(String name) {
    return objectPersistor.loadRootID(name);
  }

  public boolean containsObject(ObjectID id) {
    synchronized (extantObjectIDs) {
      assertNotInShutdown();
      return extantObjectIDs.contains(id);
    }
  }

  public void addNewObject(ManagedObject managed) {
    synchronized (extantObjectIDs) {
      assertNotInShutdown();
      boolean result = extantObjectIDs.add(managed.getID());
      Assert.eval(result);
    }
  }

  public void commitObject(PersistenceTransaction tx, ManagedObject managed) {
    assertNotInShutdown();
    objectPersistor.saveObject(tx, managed);
  }

  public void commitAllObjects(PersistenceTransaction tx, Collection managed) {
    assertNotInShutdown();
    objectPersistor.saveAllObjects(tx, managed);
  }

  public void removeAllObjectsByIDNow(PersistenceTransaction tx, Collection ids) {
    assertNotInShutdown();
    this.objectPersistor.deleteAllObjectsByID(tx, ids);
    basicRemoveAll(ids);
  }

  private void basicRemoveAll(Collection ids) {
    synchronized (extantObjectIDs) {
      this.extantObjectIDs.removeAll(ids);
    }
  }

  public Set getAllObjectIDs() {
    synchronized (extantObjectIDs) {
      assertNotInShutdown();
      return new ObjectIDSet2(this.extantObjectIDs);
    }
  }

  public ManagedObject getObjectByID(ObjectID id) {
    assertNotInShutdown();

    ManagedObject rv = this.objectPersistor.loadObjectByID(id);
    if (rv == null) return rv;
    if (rv.isDirty()) { throw new AssertionError("Object loaded from persistor is dirty.  Persistor: "
                                                 + this.objectPersistor + ", ManagedObject: " + rv); }
    return rv;
  }

  public synchronized void shutdown() {
    assertNotInShutdown();
    this.inShutdown = true;
  }

  public synchronized boolean inShutdown() {
    return this.inShutdown;
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    PrettyPrinter rv = out;
    out = out.println(getClass().getName()).duplicateAndIndent();
    out.indent().print("extantObjectIDs: ").visit(extantObjectIDs).println();
    out.indent().print("objectPersistor: ").duplicateAndIndent().visit(objectPersistor).println();
    return rv;
  }

  private void assertNotInShutdown() {
    if (this.inShutdown) throw new ShutdownError();
  }
}
