/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import org.junit.Assert;
import org.mockito.Mockito;

import com.tc.async.api.Stage;
import com.tc.async.impl.MockSink;
import com.tc.async.impl.MockStage;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerStatsListener;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.PeriodicDGCResultContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.dgc.impl.AbstractGarbageCollector;
import com.tc.objectserver.impl.GarbageCollectionManagerImpl;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class GarbageCollectHandlerTest extends TCTestCase {
  {
    // Set the client object references refresh rate higher so we don't need to wait so long for refresh
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");
  }

  private static final TCLogger    logger = TCLogging.getLogger(GarbageCollectHandlerTest.class);
  private ClientStateManager       clientStateManager;
  private GarbageCollectHandler    handler;
  private TestObjectManager        objectManager;
  private GarbageCollector         gc;
  private GarbageCollectionManager gcManager;
  private Stage                    gcStage;
  private GCThread                 gcThread;
  private MockSink                 gcSink;
  private ClientID                 testClientId;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testClientId = new ClientID(1);
    clientStateManager = new ClientStateManagerImpl(logger);
    clientStateManager.startupNode(testClientId);
    handler = new GarbageCollectHandler(new ObjectManagerConfig(10, true, false, true, false, 10, 10));
    gc = new TestGarbageCollector();
    objectManager = new TestObjectManager();
    gcStage = new MockStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE);
    gcSink = (MockSink) gcStage.getSink();
    gcManager = new GarbageCollectionManagerImpl(gcSink, new ClientObjectReferenceSet(clientStateManager));
    ServerConfigurationContext scc = Mockito.mock(ServerConfigurationContext.class);
    Mockito.when(scc.getObjectManager()).thenReturn(objectManager);
    Mockito.when(scc.getStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE)).thenReturn(gcStage);
    Mockito.when(scc.getGarbageCollectionManager()).thenReturn(gcManager);
    handler.initialize(scc);

    gcThread = new GCThread();
    gcThread.start();
  }

  public void testScheduleInlineDGC() throws Exception {
    SortedSet<ObjectID> set1 = objectIds(0, 100);
    gcManager.deleteObjects(set1);
    gcThread.waitForGCCount(1);
    Assert.assertTrue(objectManager.deletedObjects.equals(set1));

    gc.waitToDisableGC();
    SortedSet<ObjectID> set2 = objectIds(100, 200);
    gcManager.deleteObjects(set2);
    // Sleep for a bit of time since the gc thread blocks in some not-easily-checkable location
    ThreadUtil.reallySleep(10 * 1000);
    Assert.assertTrue(objectManager.deletedObjects.equals(set1));

    gc.enableGC();
    gcThread.waitForGCCount(2);
    Assert.assertTrue(objectManager.deletedObjects.equals(objectIds(0, 200)));

    gcManager.deleteObjects(objectIds(200, 300));
    gcThread.waitForGCCount(3);
    Assert.assertTrue(objectManager.deletedObjects.equals(objectIds(0, 300)));
  }

  public void testInlineDGCReferencedObject() throws Exception {
    clientStateManager.addReference(testClientId, new ObjectID(50));
    gcManager.deleteObjects(objectIds(0, 100));
    gcThread.waitForGCCount(1);
    Assert.assertFalse(objectManager.deletedObjects.contains(new ObjectID(50)));
    Assert.assertTrue(objectManager.deletedObjects.containsAll(objectIds(0, 50)));
    Assert.assertTrue(objectManager.deletedObjects.containsAll(objectIds(51, 100)));

    // Wait for the client object refresh time to elapse
    ThreadUtil.reallySleep(5 * 1000);

    // Try deleting again, since the reference is still there, delete should again fail.
    gcManager.deleteObjects(objectIds(100, 200));
    gcThread.waitForGCCount(2);
    Assert.assertFalse(objectManager.deletedObjects.contains(new ObjectID(50)));
    Assert.assertTrue(objectManager.deletedObjects.containsAll(objectIds(0, 50)));
    Assert.assertTrue(objectManager.deletedObjects.containsAll(objectIds(51, 200)));

    // Remove the referenced object, and try to delete it again
    clientStateManager.removeReferences(testClientId, Collections.singleton(new ObjectID(50)), new ObjectIDSet());
    ThreadUtil.reallySleep(5 * 1000);

    gcManager.deleteObjects(objectIds(200, 300));
    gcThread.waitForGCCount(3);
    gcManager.deleteObjects(objectIds(300, 400));
    gcThread.waitForGCCount(4);
    Assert.assertTrue(objectIds(0, 400).containsAll(objectManager.deletedObjects)
                      && objectManager.deletedObjects.containsAll(objectIds(0, 400)));
  }

  private SortedSet<ObjectID> objectIds(long start, long end) {
    SortedSet<ObjectID> oids = new ObjectIDSet();
    for (long i = start; i < end; i++) {
      oids.add(new ObjectID(i));
    }
    return oids;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    gcThread.shutdown();
  }

  private class GCThread extends Thread {
    private int              gcCount = 0;
    private volatile boolean stop    = false;

    private GCThread() {
      super("GCThread");
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        while (!stop) {
          handler.handleEvent(gcSink.take());
          synchronized (this) {
            gcCount++;
            notifyAll();
          }
        }
      } catch (AssertionError e) {
        // Hack to get around the extra AssertionError MockSink throws when interrupted
        if (!stop || !(e.getCause() instanceof InterruptedException)) { throw e; }
      }
    }

    private void shutdown() {
      stop = true;
      interrupt();
    }

    public void waitForGCCount(int count) throws InterruptedException {
      synchronized (this) {
        while (gcCount < count) {
          wait();
        }
      }
    }
  }

  private class TestObjectManager implements ObjectManager {
    private final SortedSet<ObjectID> deletedObjects = new ObjectIDSet();

    public ManagedObject getObjectByID(ObjectID id) {
      throw new ImplementMe();
    }

    public Iterator getRootNames() {
      throw new ImplementMe();
    }

    public ManagedObjectFacade lookupFacade(ObjectID id, int limit) {
      throw new ImplementMe();
    }

    public int getCachedObjectCount() {
      throw new ImplementMe();
    }

    public void stop() {
      throw new ImplementMe();
    }

    public void releaseAndCommit(PersistenceTransaction tx, ManagedObject object) {
      throw new ImplementMe();
    }

    public void releaseAllReadOnly(Collection<ManagedObject> objects) {
      throw new ImplementMe();
    }

    public void releaseReadOnly(ManagedObject object) {
      throw new ImplementMe();

    }

    public void releaseAllAndCommit(PersistenceTransaction tx, Collection<ManagedObject> collection) {
      throw new ImplementMe();
    }

    public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                 int maxCount) {
      throw new ImplementMe();
    }

    public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
      throw new ImplementMe();
    }

    public Iterator getRoots() {
      throw new ImplementMe();
    }

    public Map getRootNamesToIDsMap() {
      throw new ImplementMe();
    }

    public void createRoot(String name, ObjectID id) {
      throw new ImplementMe();
    }

    public void createNewObjects(Set<ObjectID> ids) {
      throw new ImplementMe();
    }

    public ObjectID lookupRootID(String name) {
      throw new ImplementMe();
    }

    public GarbageCollector getGarbageCollector() {
      return gc;
    }

    public void setGarbageCollector(GarbageCollector gc) {
      throw new ImplementMe();
    }

    public Set<ObjectID> getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
      throw new ImplementMe();
    }

    public void waitUntilReadyToGC() {
      throw new ImplementMe();
    }

    public int getLiveObjectCount() {
      throw new ImplementMe();
    }

    public void notifyGCComplete(PeriodicDGCResultContext periodicDGCResultContext) {
      throw new ImplementMe();
    }

    public void deleteObjects(DGCResultContext dgcResultContext) {
      deletedObjects.addAll(dgcResultContext.getGarbageIDs());
    }

    public void setStatsListener(ObjectManagerStatsListener listener) {
      throw new ImplementMe();
    }

    public void start() {
      throw new ImplementMe();
    }

    public int getCheckedOutCount() {
      throw new ImplementMe();
    }

    public Set getRootIDs() {
      throw new ImplementMe();
    }

    public ObjectIDSet getAllObjectIDs() {
      throw new ImplementMe();
    }

    public ObjectIDSet getObjectIDsInCache() {
      throw new ImplementMe();
    }

    public void addFaultedObject(ObjectID oid, ManagedObject mo, boolean removeOnRelease) {
      throw new ImplementMe();
    }

    public void flushAndEvict(List objects2Flush) {
      throw new ImplementMe();
    }

    public void preFetchObjectsAndCreate(Set<ObjectID> oids, Set<ObjectID> newOids) {
      throw new ImplementMe();
    }

    public ManagedObject getObjectByIDOrNull(ObjectID id) {
      throw new ImplementMe();
    }

    public ManagedObject getQuietObjectByID(ObjectID id) {
      throw new ImplementMe();
    }
  }

  private class TestGarbageCollector extends AbstractGarbageCollector {
    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      throw new ImplementMe();
    }

    public void doGC(GCType type) {
      // no-op
    }

    public void start() {
      // no-op
    }

    public void stop() {
      // no-op
    }

    public boolean isStarted() {
      return false;
    }

    public void setState(LifeCycleState st) {
      // no-op
    }

    public void addListener(GarbageCollectorEventListener listener) {
      // no-op
    }

    public boolean deleteGarbage(PeriodicDGCResultContext resultContext) {
      // no-op
      return false;
    }

    public void notifyObjectCreated(ObjectID id) {
      // no-op
    }

    public void notifyNewObjectInitalized(ObjectID id) {
      // no-op
    }

    public void notifyObjectsEvicted(Collection evicted) {
      // no-op
    }
  }
}
