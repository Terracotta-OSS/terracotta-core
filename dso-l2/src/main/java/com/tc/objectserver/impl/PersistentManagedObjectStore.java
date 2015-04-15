/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
