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
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.InlineGCContext;
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
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private PersistenceTransactionProvider persistenceTransactionProvider;

  public GarbageCollectHandlerTest() {
    timebombTest("2013-02-10");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    inlineGCCount = new AtomicInteger();
    periodicDGCCount = new AtomicInteger();
    deletedObjects = new ObjectIDSet();
    testClientId = new ClientID(1);
    clientStateManager = new ClientStateManagerImpl(logger);
    clientStateManager.startupNode(testClientId);
    persistenceTransactionProvider = mock(PersistenceTransactionProvider.class);
    Transaction transaction = mock(Transaction.class);
    when(persistenceTransactionProvider.newTransaction()).thenReturn(transaction);
    handler = new GarbageCollectHandler(new ObjectManagerConfig(10, true, true, false),
                                        new GarbageCollectionInfoPublisherImpl(), persistenceTransactionProvider);
    gc = new TestGarbageCollector();
    objectManager = new TestObjectManager();
    gcStage = new MockStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE);
    gcSink = (MockSink) gcStage.getSink();
    gcManager = spy(new ActiveGarbageCollectionManager(gcSink));
    ServerConfigurationContext scc = mock(ServerConfigurationContext.class);
    Mockito.when(scc.getObjectManager()).thenReturn(objectManager);
    Mockito.when(scc.getStage(ServerConfigurationContext.GARBAGE_COLLECT_STAGE)).thenReturn(gcStage);
    Mockito.when(scc.getGarbageCollectionManager()).thenReturn(gcManager);
    Mockito.when(scc.getTransactionManager()).thenReturn(new MyServerTransactionManager());
    handler.initialize(scc);
    gcManager.initializeContext(scc);

    gcThread = new GCThread();
    gcThread.start();
  }

  public void testScheduleInline() throws Exception {
    gcSink.add(new InlineGCContext());
    ThreadUtil.reallySleep(2 * 1000);
    ObjectIDSet oids = new ObjectIDSet(Collections.singleton(new ObjectID(1)));
    when(gcManager.nextObjectsToDelete()).thenReturn(oids);
    verify(gcManager).nextObjectsToDelete();
    assertThat(deletedObjects, hasItem(new ObjectID(1)));
    verify(gcManager).missingObjectsToDelete(anySet());
  }

  public void testSchedulePeriodicDGC() throws Exception {
    long start = System.nanoTime();
    gcSink.add(new PeriodicGarbageCollectContext(GCType.FULL_GC, 5 * 1000));
    gcThread.waitForPeriodicDGCCount(3);
    long finish = System.nanoTime();
    long timeTaken = NANOSECONDS.toSeconds(finish - start);
    Assert.assertTrue("Did not finish 3 GC's in 15 seconds.", timeTaken >= 13 && timeTaken <= 17);
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

    @Override
    public ManagedObject getObjectByID(ObjectID id) {
      throw new ImplementMe();
    }

    @Override
    public Iterator getRootNames() {
      throw new ImplementMe();
    }

    @Override
    public ManagedObjectFacade lookupFacade(ObjectID id, int limit) {
      throw new ImplementMe();
    }

    @Override
    public void stop() {
      throw new ImplementMe();
    }

    @Override
    public void release(ManagedObject object) {
      throw new ImplementMe();
    }

    @Override
    public void releaseAllReadOnly(Collection<ManagedObject> objects) {
      throw new ImplementMe();
    }

    @Override
    public void releaseReadOnly(ManagedObject object) {
      throw new ImplementMe();

    }

    @Override
    public void releaseAll(Collection<ManagedObject> collection) {
      throw new ImplementMe();
    }

    @Override
    public boolean lookupObjectsAndSubObjectsFor(NodeID nodeID, ObjectManagerResultsContext responseContext,
                                                 int maxCount) {
      throw new ImplementMe();
    }

    @Override
    public boolean lookupObjectsFor(NodeID nodeID, ObjectManagerResultsContext context) {
      throw new ImplementMe();
    }

    @Override
    public Iterator getRoots() {
      throw new ImplementMe();
    }

    @Override
    public Map getRootNamesToIDsMap() {
      throw new ImplementMe();
    }

    @Override
    public void createRoot(String name, ObjectID id) {
      throw new ImplementMe();
    }

    @Override
    public void createNewObjects(Set<ObjectID> ids) {
      throw new ImplementMe();
    }

    @Override
    public ObjectID lookupRootID(String name) {
      throw new ImplementMe();
    }

    @Override
    public GarbageCollector getGarbageCollector() {
      return gc;
    }

    @Override
    public void setGarbageCollector(GarbageCollector gc) {
      throw new ImplementMe();
    }

    @Override
    public Set<ObjectID> getObjectReferencesFrom(ObjectID id, boolean cacheOnly) {
      throw new ImplementMe();
    }

    @Override
    public void waitUntilReadyToGC() {
      throw new ImplementMe();
    }

    @Override
    public int getLiveObjectCount() {
      throw new ImplementMe();
    }

    @Override
    public void notifyGCComplete(DGCResultContext dgcResultContext) {
      deletedObjects.addAll(dgcResultContext.getGarbageIDs());
      inlineGCCount.incrementAndGet();
    }

    @Override
    public void setStatsListener(ObjectManagerStatsListener listener) {
      throw new ImplementMe();
    }

    @Override
    public void start() {
      throw new ImplementMe();
    }

    @Override
    public int getCheckedOutCount() {
      throw new ImplementMe();
    }

    @Override
    public Set getRootIDs() {
      throw new ImplementMe();
    }

    @Override
    public ObjectIDSet getAllObjectIDs() {
      throw new ImplementMe();
    }

    @Override
    public ObjectIDSet getObjectIDsInCache() {
      throw new ImplementMe();
    }

    @Override
    public ManagedObject getObjectByIDReadOnly(ObjectID id) {
      throw new ImplementMe();
    }

    @Override
    public Set<ObjectID> deleteObjects(Set<ObjectID> objectsToDelete) {
      deletedObjects.addAll(objectsToDelete);
      inlineGCCount.incrementAndGet();
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<ObjectID> tryDeleteObjects(final Set<ObjectID> objectsToDelete) {
      deletedObjects.addAll(objectsToDelete);
      return Collections.EMPTY_SET;
    }
  }

  private class TestGarbageCollector extends AbstractGarbageCollector {
    @Override
    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      throw new ImplementMe();
    }

    @Override
    public void doGC(GCType type) {
      periodicDGCCount.incrementAndGet();
    }

    @Override
    public void start() {
      // no-op
    }

    @Override
    public void stop() {
      // no-op
    }

    @Override
    public boolean isStarted() {
      return false;
    }

    @Override
    public void setState(LifeCycleState st) {
      // no-op
    }

    @Override
    public void addListener(GarbageCollectorEventListener listener) {
      // no-op
    }

    @Override
    public void deleteGarbage(DGCResultContext resultContext) {
      // no-op
    }
  }

  private class MyServerTransactionManager extends TestServerTransactionManager {
    @Override
    public void callBackOnResentTxnsInSystemCompletion(TxnsInSystemCompletionListener l) {
      l.onCompletion();
    }
  }
}
