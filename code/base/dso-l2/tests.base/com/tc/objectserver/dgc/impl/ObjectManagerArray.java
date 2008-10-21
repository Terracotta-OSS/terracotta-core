/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.impl.InMemoryManagedObjectStore;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ObjectManagerArray implements ObjectManager {

  private final int                 arrayCount;
  private final ObjectManagerImpl[] objectManagers;
  private final ClientStateManager  clientStateManager;
  private final ThreadGroup         tg;
  private final ObjectManagerConfig config;
  private GarbageCollector          collector;

  public ObjectManagerArray(int arrayCount, ClientStateManager clientStateManager) {
    Assert.eval(arrayCount > 0);
    this.arrayCount = arrayCount;
    this.objectManagers = new ObjectManagerImpl[arrayCount];
    this.tg = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(ObjectManagerImpl.class)));   
    this.clientStateManager = clientStateManager; //new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    this.config = new TestObjectManagerConfig(0, false);
    
    initObjectManagers();
  }

  public void initObjectManagers() {
    for (int i = 0; i < arrayCount; i++) {
      objectManagers[i] = createObjectManager();
    }
  }

  public ObjectManagerImpl[] getObjectManagers() {
    return objectManagers;
  }

  public void createObjects(Set<ObjectID> ids) {
    createObjects(ids, new HashSet<ObjectID>());
  }

  public void createObjects(Set<ObjectID> ids, Set<ObjectID> children) {
    for (Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      ObjectID id = iter.next();
      TestManagedObject mo = new TestManagedObject(id, children.toArray(new ObjectID[children.size()]));
      Random rand = new Random();
      int index = rand.nextInt(arrayCount);
      objectManagers[index].createObject(mo);
      objectManagers[index].getObjectStore().addNewObject(mo);
    }
  }

  private ObjectManagerImpl createObjectManager() {
    ManagedObjectStore store = new InMemoryManagedObjectStore(new HashMap());
    EvictionPolicy cache = new NullCache();
    PersistenceTransactionProvider persistenceTransactionalProvider = new TestPersistenceTransactionProvider();
    TestSink faultSink = new TestSink();
    TestSink flushSink = new TestSink();
    ObjectManagerImpl objectManager = new ObjectManagerImpl(config, tg, clientStateManager, store, cache,
                                                            persistenceTransactionalProvider, faultSink, flushSink);
    return objectManager;
  }

  private static class TestObjectManagerConfig extends ObjectManagerConfig {

    public long    myGCThreadSleepTime = 100;
    public boolean paranoid;

    public TestObjectManagerConfig() {
      super(10000, true, true, true, false, 60000);
    }

    TestObjectManagerConfig(long gcThreadSleepTime, boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, false, 60000);
    }

    @Override
    public long gcThreadSleepTime() {
      return myGCThreadSleepTime;
    }

    @Override
    public boolean paranoid() {
      return paranoid;
    }
  }

  public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
    //
  }

  public void createNewObjects(Set<ObjectID> ids) {
    //
  }

  public void createRoot(String name, ObjectID id) {
    //
  }

  public void flushAndEvict(List objects2Flush) {
    //
  }

  public ObjectIDSet getAllObjectIDs() {
    return null;
  }

  public int getCheckedOutCount() {
    return 0;
  }

  public GarbageCollector getGarbageCollector() {
    return collector;
  }

  public ManagedObject getObjectByIDOrNull(ObjectID id) {
   return null;
  }

  public ManagedObject getObjectFromCacheByIDOrNull(ObjectID id) {
    return null;
  }

  public ObjectIDSet getObjectIDsInCache() {
    return null;
  }

  public Set getRootIDs() {
    return null;
  }

  public Map getRootNamesToIDsMap() {
    return null;
  }

  public Iterator getRoots() {
     return null;
  }

  public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext, int maxCount) {
     return false;
  }

  public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
    return false;
  }

  public ObjectID lookupRootID(String name) {
    return null;
  }

  public void notifyGCComplete(GCResultContext resultContext) {
    for(int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].notifyGCComplete(resultContext);
    }
  }

  public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids) {
  //
  }

  public void release(PersistenceTransaction tx, ManagedObject object) {
  //
  }

  public void releaseAll(PersistenceTransaction tx, Collection<ManagedObject> collection) {
  //
  }

  public void releaseAllReadOnly(Collection<ManagedObject> objects) {
   //
  }

  public void releaseReadOnly(ManagedObject object) {
   //
  }

  public void setGarbageCollector(final GarbageCollector gc) {
    this.collector = gc;
    
    for(int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].setGarbageCollector(gc);
    }
//    StoppableThread st = new GarbageCollectorThread(this.tg, "DGC", gc, this.config);
//    st.setDaemon(true);
//    gc.setState(st);
  }
  

  public void setStatsListener(ObjectManagerStatsListener listener) {
    //
  }

  public void start() {
  //
    for(int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].start();
    }
  }

  public void stop() {
    for(int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].stop();
    }
  }

  public void waitUntilReadyToGC() {
    for(int i = 0; i < objectManagers.length; i++) {
      objectManagers[i].waitUntilReadyToGC();
    }
  }

  public ManagedObject getObjectByID(ObjectID id) {
    return null;
  }

}
