/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
import com.tc.util.ObjectIDSet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistentManagedObjectStoreTest extends TestCase {

  private Map                          map;
  private Sink                         gcSink;
  private ManagedObjectPersistor persistor;
  private PersistentManagedObjectStore objectStore;

  protected void setUp() throws Exception {
    super.setUp();
    map = new HashMap();
    gcSink = mock(Sink.class);
    persistor = mock(ManagedObjectPersistor.class);
    objectStore = new PersistentManagedObjectStore(persistor, gcSink);
  }

  public void testGetObjectByID() throws Exception {
    ObjectID objectID = new ObjectID(1);
    objectStore.getObjectByID(objectID);
    verify(persistor).loadObjectByID(objectID);
  }

  public void testContainsObject() throws Exception {
    ObjectID objectID = new ObjectID(1);
    objectStore.containsObject(objectID);
    verify(persistor).containsObject(objectID);
  }

  public void testRemoveAllObjectsByIDNow() throws Exception {
    ObjectIDSet objectIDs = new ObjectIDSet(Arrays.asList(new ObjectID(1), new ObjectID(2)));
    objectStore.removeAllObjectsByIDNow(objectIDs);
    verify(persistor).deleteAllObjects(objectIDs);
  }

  public void testRemoveObjectIDs() throws Exception {
    ObjectIDSet objectIDs = new ObjectIDSet(Arrays.asList(new ObjectID(1), new ObjectID(2)));
    DGCResultContext dgcResultContext = new DGCResultContext(objectIDs, GarbageCollectionInfo.NULL_INFO);
    objectStore.removeAllObjectsByID(dgcResultContext);
    verify(persistor).deleteAllObjects(objectIDs);
    verify(gcSink).add(dgcResultContext);
  }
}
