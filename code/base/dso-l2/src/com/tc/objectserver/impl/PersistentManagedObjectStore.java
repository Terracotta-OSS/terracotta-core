/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ShutdownError;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class PersistentManagedObjectStore implements ManagedObjectStore {

  private final ManagedObjectPersistor objectPersistor;
  private final Sink                   gcDisposerSink;
  private volatile boolean             inShutdown;

  public PersistentManagedObjectStore(final ManagedObjectPersistor persistor, final Sink gcDisposerSink) {
    this.objectPersistor = persistor;
    this.gcDisposerSink = gcDisposerSink;
  }

  public int getObjectCount() {
    return this.objectPersistor.getObjectCount();
  }

  public long nextObjectIDBatch(final int batchSize) {
    final long rv = this.objectPersistor.nextObjectIDBatch(batchSize);
    return rv;
  }

  public long currentObjectIDValue() {
    return this.objectPersistor.currentObjectIDValue();
  }

  public void setNextAvailableObjectID(final long startID) {
    this.objectPersistor.setNextAvailableObjectID(startID);
  }

  public void addNewRoot(final PersistenceTransaction tx, final String rootName, final ObjectID id) {
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

  public void addNewObject(final ManagedObject managed) {
    assertNotInShutdown();
    final boolean result = this.objectPersistor.addNewObject(managed);
    Assert.eval(result);
  }

  public void commitObject(final PersistenceTransaction tx, final ManagedObject managed) {
    assertNotInShutdown();
    this.objectPersistor.saveObject(tx, managed);
  }

  public void commitAllObjects(final PersistenceTransaction tx, final Collection managed) {
    assertNotInShutdown();
    this.objectPersistor.saveAllObjects(tx, managed);
  }

  public void removeAllObjectsByIDNow(final SortedSet<ObjectID> ids) {
    assertNotInShutdown();
    this.objectPersistor.deleteAllObjects(ids);
  }

  /**
   * This method is used by the GC to trigger removing Garbage.
   */
  public void removeAllObjectsByID(final GCResultContext gcResult) {
    assertNotInShutdown();
    // NOTE:: Calling removeAllObjectIDs to remove the object IDs in-line so that the next DGC cycle doesn't pick up
    // the same GCed object IDs again if the delete stage is falling behind.
    this.objectPersistor.removeAllObjectIDs(gcResult.getGCedObjectIDs());
    this.gcDisposerSink.add(gcResult);
  }

  public ObjectIDSet getAllObjectIDs() {
    assertNotInShutdown();
    return this.objectPersistor.snapshotObjectIDs();
  }

  public ObjectIDSet getAllEvictableObjectIDs() {
    assertNotInShutdown();
    return this.objectPersistor.snapshotEvictableObjectIDs();
  }
  
  public ObjectIDSet getAllMapTypeObjectIDs() {
    assertNotInShutdown();
    return this.objectPersistor.snapshotMapTypeObjectIDs();
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

  public boolean inShutdown() {
    return this.inShutdown;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    final PrettyPrinter rv = out;
    out = out.println(getClass().getName()).duplicateAndIndent();
    out = out.println("object count: " + this.objectPersistor.getObjectCount()).duplicateAndIndent();
    return rv;
  }

  private void assertNotInShutdown() {
    if (this.inShutdown) { throw new ShutdownError(); }
  }
}
