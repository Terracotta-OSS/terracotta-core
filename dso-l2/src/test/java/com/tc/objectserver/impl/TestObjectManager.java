/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GCStatsEventListener;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectStatsManager;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestObjectManager implements ObjectManager, ObjectStatsManager {

  private final Set<ObjectID> existingObjectIDs = new ObjectIDSet();
  private final Map<ObjectID, ManagedObject> checkedOutObjects = new HashMap<ObjectID, ManagedObject>();
  private List<ObjectManagerResultsContext> pendingLookups = new ArrayList<ObjectManagerResultsContext>();

  public void addExistingObjectIDs(Collection<ObjectID> objectIDs) {
    existingObjectIDs.addAll(objectIDs);
  }

  @Override
  public void stop() {
    throw new ImplementMe();
  }

  @Override
  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext context, int maxCount) {
    return lookupObjectsFor(nodeID, context);
  }

  @Override
  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    for (ObjectID oid : context.getLookupIDs()) {
      if (checkedOutObjects.containsKey(oid)) {
        pendingLookups.add(context);
        return false;
      }
    }
    context.setResults(new ObjectManagerLookupResultsImpl(createLookResults(context.getLookupIDs()),
                       TCCollections.EMPTY_OBJECT_ID_SET,
                       missingObjects(context.getLookupIDs())));
    return true;
  }

  private void processPending() {
    List<ObjectManagerResultsContext> lookupsToProcess = pendingLookups;
    pendingLookups = new ArrayList<ObjectManagerResultsContext>();
    for (ObjectManagerResultsContext lookupToProcess : lookupsToProcess) {
      lookupObjectsFor(new ClientID(0), lookupToProcess);
    }
  }

  private Map<ObjectID, ManagedObject> createLookResults(Collection<ObjectID> ids) {
    Map<ObjectID, ManagedObject> results = new HashMap<ObjectID, ManagedObject>();
    for (final ObjectID id : ids) {
      if (existingObjectIDs.contains(id)) {
        TestManagedObject tmo = new TestManagedObject(id);
        results.put(id, tmo);
        checkedOutObjects.put(id, tmo);
      }
    }
    return results;
  }

  private ObjectIDSet missingObjects(Collection<ObjectID> ids) {
    ObjectIDSet missingObjects = new ObjectIDSet(ids);
    missingObjects.removeAll(existingObjectIDs);
    return missingObjects;
  }

  @Override
  public Iterator getRoots() {
    throw new ImplementMe();
  }

  @Override
  public void createRoot(String name, ObjectID id) {
    //
  }

  @Override
  public ObjectID lookupRootID(String name) {
    throw new ImplementMe();
  }

  @Override
  public void setGarbageCollector(GarbageCollector gc) {
    throw new ImplementMe();
  }

  public void addListener(GCStatsEventListener listener) {
    throw new ImplementMe();
  }

  @Override
  public ManagedObject getObjectByID(ObjectID id) {
    if (checkedOutObjects.containsKey(id)) {
      throw new AssertionError("Object is already checked out!");
    } else {
      ManagedObject mo = new TestManagedObject(id);
      checkedOutObjects.put(id, mo);
      return mo;
    }
  }

  @Override
  public void release(ManagedObject object) {
    Assert.assertNotNull(checkedOutObjects.remove(object.getID()));
    processPending();
  }

  @Override
  public void releaseReadOnly(ManagedObject object) {
    Assert.assertNotNull(checkedOutObjects.remove(object.getID()));
    processPending();
  }

  @Override
  public void releaseAll(Collection<ManagedObject> collection) {
    for (ManagedObject managedObject : collection) {
      Assert.assertNotNull(checkedOutObjects.remove(managedObject.getID()));
    }
    processPending();
  }

  public PrettyPrinterImpl prettyPrint(PrettyPrinterImpl out) {
    throw new ImplementMe();
  }

  public final NoExceptionLinkedQueue startCalls = new NoExceptionLinkedQueue();

  @Override
  public void start() {
    startCalls.put(new Object());
  }

  @Override
  public void releaseAllReadOnly(Collection<ManagedObject> objects) {
    releaseAll(objects);
  }

  @Override
  public int getCheckedOutCount() {
    return 0;
  }

  @Override
  public Set getRootIDs() {
    return new HashSet();
  }

  @Override
  public ObjectIDSet getAllObjectIDs() {
    return new ObjectIDSet();
  }

  public Object getLock() {
    return this;
  }

  @Override
  public void waitUntilReadyToGC() {
    throw new ImplementMe();
  }

  @Override
  public void notifyGCComplete(DGCResultContext dgcResultContext) {
    throw new ImplementMe();
  }

  @Override
  public Set<ObjectID> deleteObjects(Set<ObjectID> objectsToDelete) {
    throw new ImplementMe();
  }

  @Override
  public Map getRootNamesToIDsMap() {
    throw new ImplementMe();
  }

  @Override
  public GarbageCollector getGarbageCollector() {
    throw new ImplementMe();
  }

  public String dump() {
    throw new ImplementMe();
  }

  public void dump(Writer writer) {
    throw new ImplementMe();

  }

  @Override
  public void createNewObjects(Set ids) {
    addExistingObjectIDs(ids);
  }

  @Override
  public ManagedObject getObjectByIDReadOnly(ObjectID id) {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDSet getObjectIDsInCache() {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDSet getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
    throw new ImplementMe();
  }

  @Override
  public String getObjectTypeFromID(ObjectID id) {
    return "";
  }

  @Override
  public int getLiveObjectCount() {
    return 0;
  }

  @Override
  public Iterator getRootNames() {
    return null;
  }

  @Override
  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) {
    return null;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public Set<ObjectID> tryDeleteObjects(final Set<ObjectID> objectsToDelete, final Set<ObjectID> coObjects) {
    return Collections.EMPTY_SET;
  }
}
