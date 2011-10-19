/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

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
import com.tc.objectserver.context.PeriodicGarbageCollectContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.dgc.api.GarbageCollector;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.dgc.api.GarbageCollectorEventListener;
import com.tc.objectserver.dgc.impl.AbstractGarbageCollector;
import com.tc.objectserver.dgc.impl.GarbageCollectionInfoPublisherImpl;
import com.tc.objectserver.impl.ActiveGarbageCollectionManager;
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
import java.util.concurrent.atomic.AtomicInteger;

public class GarbageCollectHandlerTest extends TCTestCase {
  {
    // Set the client object references refresh rate higher so we don't need to wait so long for refresh
    TCPropertiesImpl.getProperties()
        .setProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "100");
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS, "1");
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_MAX_OBJECTS, "500");
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
  private SortedSet<ObjectID>      deletedObjects;
  private AtomicInteger            inlineGCCount;
  private AtomicInteger            periodicDGCCount;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    inlineGCCount = new AtomicInteger();
    periodicDGCCount = new AtomicInteger();
    deletedObjects = new ObjectIDSet();
    testClientId = new ClientID(1);
    clientStateManager = new ClientStateManagerImpl(logger);
    clientStateManager.startupNode(testClientId);
    handler = new GarbageCollectHandler(new ObjectManagerConfig(10, true, false, true, false, 10, 10),
                                        new GarbageCollectionInfoPublisherImpl());
    gc = new TestGarbageCollector();
    objectManager = new TestObjectManager();
    gcStage = new MockStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE);
    gcSink = (MockSink) gcStage.getSink();
    gcManager = new ActiveGarbageCollectionManager(gcSink, new ClientObjectReferenceSet(clientStateManager));
    ServerConfigurationContext scc = Mockito.mock(ServerConfigurationContext.class);
    Mockito.when(scc.getObjectManager()).thenReturn(objectManager);
    Mockito.when(scc.getStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE)).thenReturn(gcStage);
    Mockito.when(scc.getGarbageCollectionManager()).thenReturn(gcManager);
    handler.initialize(scc);

    gcThread = new GCThread();
    gcThread.start();
  }

  public void testScheduleInlineDGC() throws Exception {
    // Wait a bit for inline-dgc interval
    ThreadUtil.reallySleep(5 * 1000);

    SortedSet<ObjectID> set1 = objectIds(0, 100);
    gcManager.deleteObjects(set1);
    gcThread.waitForInlineDGCCount(1);
    Assert.assertTrue(deletedObjects.equals(set1));

    ThreadUtil.reallySleep(5 * 1000);
    gc.waitToDisableGC();
    SortedSet<ObjectID> set2 = objectIds(100, 200);
    gcManager.deleteObjects(set2);
    // Sleep for a bit of time since the gc thread blocks in some not-easily-checkable location
    ThreadUtil.reallySleep(10 * 1000);
    Assert.assertTrue(deletedObjects.equals(set1));

    gc.enableGC();
    gcThread.waitForInlineDGCCount(2);
    Assert.assertTrue(deletedObjects.equals(objectIds(0, 200)));

    ThreadUtil.reallySleep(5 * 1000);
    gcManager.deleteObjects(objectIds(200, 300));
    gcThread.waitForInlineDGCCount(3);
    Assert.assertTrue(deletedObjects.equals(objectIds(0, 300)));
  }

  public void testInlineDGCReferencedObject() throws Exception {
    ThreadUtil.reallySleep(5 * 1000);

    clientStateManager.addReference(testClientId, new ObjectID(50));
    gcManager.deleteObjects(objectIds(0, 100));
    gcThread.waitForInlineDGCCount(1);
    Assert.assertFalse(deletedObjects.contains(new ObjectID(50)));
    Assert.assertTrue(deletedObjects.containsAll(objectIds(0, 50)));
    Assert.assertTrue(deletedObjects.containsAll(objectIds(51, 100)));

    // Wait for the client object refresh time to elapse
    ThreadUtil.reallySleep(1000);

    // Try deleting again, since the reference is still there, delete should again fail.
    gcManager.deleteObjects(objectIds(100, 200));
    gcThread.waitForInlineDGCCount(2);
    Assert.assertFalse(deletedObjects.contains(new ObjectID(50)));
    Assert.assertTrue(deletedObjects.containsAll(objectIds(0, 50)));
    Assert.assertTrue(deletedObjects.containsAll(objectIds(51, 200)));

    // Remove the referenced object, and try to delete it again
    clientStateManager.removeReferences(testClientId, Collections.singleton(new ObjectID(50)), new ObjectIDSet());
    ThreadUtil.reallySleep(5 * 1000);

    gcManager.deleteObjects(objectIds(200, 300));
    gcThread.waitForInlineDGCCount(3);

    ThreadUtil.reallySleep(5 * 1000);
    gcManager.deleteObjects(objectIds(300, 400));
    gcThread.waitForInlineDGCCount(4);

    if (!objectIds(0, 400).containsAll(deletedObjects) || !deletedObjects.containsAll(objectIds(0, 400))) {
      Assert.fail("Deleted objectIds do not match. Expected=" + objectIds(0, 400) + " actual=" + deletedObjects);
    }
  }

  public void testSchedulePeriodicDGC() throws Exception {
    long start = System.nanoTime();
    gcSink.add(new PeriodicGarbageCollectContext(GCType.FULL_GC, 5 * 1000));
    gcThread.waitForPeriodicDGCCount(3);
    long finish = System.nanoTime();
    long timeTaken = NANOSECONDS.toSeconds(finish - start);
    Assert.assertTrue("Did not finish 3 GC's in 15 seconds.", timeTaken >= 13 && timeTaken <= 17);
  }

  public void testBatchInlineDGC() throws Exception {
    // below the batch limit so it shouldn't schedule an immediate inline dgc
    gcManager.deleteObjects(objectIds(0, 499));
    ThreadUtil.reallySleep(10 * 1000);
    Assert.assertEquals(0, periodicDGCCount.get());

    // Trigger the inline dgc to reset the timer
    gcManager.deleteObjects(objectIds(499, 500));
    gcThread.waitForInlineDGCCount(1);

    // Should trip batch limit and immediately schedule an inline dgc
    gcManager.deleteObjects(objectIds(500, 1001));
    gcThread.waitForInlineDGCCount(2);

    // Make sure everything was actually deleted
    Assert.assertTrue(objectIds(0, 1001).containsAll(deletedObjects) && deletedObjects.containsAll(objectIds(0, 1001)));
  }

  public void testScheduleOneOffDGC() throws Exception {
    gcManager.scheduleGarbageCollection(GCType.FULL_GC);
    gcThread.waitForPeriodicDGCCount(1);

    ThreadUtil.reallySleep(10 * 1000);
    Assert.assertEquals(1, periodicDGCCount.get());
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
    private volatile boolean stop = false;

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
            notifyAll();
          }
        }
      } catch (AssertionError e) {
        // Hack to get around the extra AssertionError MockSink throws when interrupted
        if (!stop || !(e.getCause() instanceof InterruptedException)) { throw e; }
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    private void shutdown() {
      stop = true;
      interrupt();
    }

    public void waitForInlineDGCCount(int count) throws InterruptedException {
      synchronized (this) {
        while (inlineGCCount.get() < count) {
          wait();
        }
      }
    }

    public void waitForPeriodicDGCCount(int count) throws InterruptedException {
      synchronized (this) {
        while (periodicDGCCount.get() < count) {
          wait();
        }
      }
    }
  }

  private class TestObjectManager implements ObjectManager {

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

    public void notifyGCComplete(DGCResultContext dgcResultContext) {
      deletedObjects.addAll(dgcResultContext.getGarbageIDs());
      inlineGCCount.incrementAndGet();
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
      periodicDGCCount.incrementAndGet();
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

    public boolean deleteGarbage(DGCResultContext resultContext) {
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
