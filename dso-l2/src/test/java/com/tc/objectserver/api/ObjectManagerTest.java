/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tc.exception.ImplementMe;
import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.HeapStorageManagerFactory;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.Persistor;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author steve
 */
public class ObjectManagerTest extends TCTestCase {
  static {
    TCLogging.getLogger(ObjectManager.class).setLevel(LogLevelImpl.DEBUG);
  }

  private ObjectManagerImpl                  objectManager;
  private TestObjectManagerConfig            config;
  private ClientStateManager                 clientStateManager;
  private PersistentManagedObjectStore       objectStore;
  private TCLogger                           logger;
  private ObjectManagerStatsImpl             stats;
  private SampledCounter                     newObjectCounter;
  private PersistenceTransactionProvider persistenceTransactionProvider;
  private Persistor persistor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.persistor = new Persistor(HeapStorageManagerFactory.INSTANCE);
    persistor.start();
    this.logger = TCLogging.getLogger(getClass());
    this.config = new TestObjectManagerConfig();
    this.clientStateManager = new ClientStateManagerImpl();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);
    this.newObjectCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.stats = new ObjectManagerStatsImpl(this.newObjectCounter);
    this.persistenceTransactionProvider = mock(PersistenceTransactionProvider.class);
    Transaction tx = mock(Transaction.class);
    when(persistenceTransactionProvider.newTransaction()).thenReturn(tx);
  }

  private void initObjectManager() {
    this.objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    initObjectManager(this.objectStore);
  }

  private void initObjectManager(final PersistentManagedObjectStore store) {
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store,
        stats, this.persistenceTransactionProvider);
  }

  public void testShutdownAndSetGarbageCollector() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.setGarbageCollector(null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookup() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.getObjectByID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok;
    }
  }

  public void testShutdownAndLookupRootID() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.lookupRootID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateRoot() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.createRoot(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateObject() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.createObject(null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndGetRoots() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.getRoots();
      fail("Should have thrown a ShutdownError");
    } catch (final ShutdownError e) {
      // ok.
    }

  }

  public void testShutdownAndLookupObjectsForCreateIfNecessary() throws Exception {
    initObjectManager();

    this.objectManager.stop();

    try {
      this.objectManager.lookupObjectsFor(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookupObjectsFor() throws Exception {
    initObjectManager();

    this.objectManager.stop();

    try {
      this.objectManager.lookupObjectsAndSubObjectsFor(null, null, -1);
      fail("Should have thrown a ShutdownError.");
    } catch (final ShutdownError e) {
      // ok.
    }
  }

  // DEV-2324
  public void testReachableObjects() {
    this.config.paranoid = true;
    initObjectManager();

    // each object has 1000 distinct reachable objects
    createObjects(0, 1, createObjects(1000, 2000, new HashSet<ObjectID>()));
    createObjects(1, 2, createObjects(2000, 3000, new HashSet<ObjectID>()));
    createObjects(2, 3, createObjects(3000, 4000, new HashSet<ObjectID>()));
    createObjects(3, 4, createObjects(4000, 5000, new HashSet<ObjectID>()));
    createObjects(4, 5, createObjects(5000, 6000, new HashSet<ObjectID>()));
    createObjects(5, 6, createObjects(6000, 7000, new HashSet<ObjectID>()));
    createObjects(6, 7, createObjects(7000, 8000, new HashSet<ObjectID>()));
    createObjects(7, 8, createObjects(8000, 9000, new HashSet<ObjectID>()));
    createObjects(8, 9, createObjects(9000, 10000, new HashSet<ObjectID>()));
    createObjects(9, 10, createObjects(10000, 11000, new HashSet<ObjectID>()));

    // evict cache is not done. So, all objects reside in cache

    ObjectIDSet ids = makeObjectIDSet(0, 10);
    TestResultsContext results = new TestResultsContext(ids, new BitSetObjectIDSet());

    final ClientID c1 = new ClientID(1);
    // fetch 10 objects and with fault-count -1
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, -1);
    Assert.assertEquals(10, results.objects.size());
    this.objectManager.releaseAll(results.objects.values());

    // fetch 10 objects and with fault-count 1K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 1000);
    Assert.assertEquals(1000, results.objects.size());
    this.objectManager.releaseAll(results.objects.values());

    // fetch 10 objects and with fault-count 10K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 10000);
    Assert.assertEquals(10000, results.objects.size());
    this.objectManager.releaseAll(results.objects.values());

    // fetch 10 objects and with fault-count 20K. but, max objects available are 10010
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 20000);
    Assert.assertEquals(10010, results.objects.size());
    this.objectManager.releaseAll(results.objects.values());

    // single object reaching more objects than fault count
    createObjects(10, 11, createObjects(11000, 18000, new HashSet<ObjectID>()));

    ids = makeObjectIDSet(10, 11);
    results = new TestResultsContext(ids, new BitSetObjectIDSet());

    // fetch 1 object and with fault-count 5K. but, object can reach 7K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 5000);
    Assert.assertEquals(5000, results.objects.size());
    this.objectManager.releaseAll(results.objects.values());

  }

  public void testMissingObjects() {

    initObjectManager();

    createObjects(10);

    // Look up two existing objects
    final ObjectIDSet ids = makeObjectIDSet(1, 2);
    final TestResultsContext result1 = new TestResultsContext(ids, new BitSetObjectIDSet());

    this.objectManager.lookupObjectsAndSubObjectsFor(null, result1, -1);
    result1.waitTillComplete();

    // Now look two missing objects
    final ObjectIDSet missingids = makeObjectIDSet(20, 22);
    final TestResultsContext result2 = new TestResultsContext(missingids, new BitSetObjectIDSet());

    this.objectManager.lookupObjectsAndSubObjectsFor(null, result2, -1);
    result2.waitTillComplete();
    assertEquals(missingids, result2.missing);

    // Now release the first two objects
    this.objectManager.releaseAll(result1.objects.values());
  }

  public void testNewObjectIDs() {
    // this test is to make sure that the list of newly created objects IDs is
    // accurate in the lookup results
    initObjectManager();

    final ObjectIDSet ids = new BitSetObjectIDSet(); // important to use a Set here

    ObjectID id1;
    ids.add((id1 = new ObjectID(1)));
    ObjectID id2;
    ids.add((id2 = new ObjectID(2)));
    final ClientID key = new ClientID(0);

    this.objectManager.createNewObjects(ids);

    TestResultsContext results = new TestResultsContext(ids, ids);
    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(2, results.objects.size());

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();

    ManagedObject mo = results.objects.get(id1);
    TestMapDNA ta;
    mo.apply((ta = new TestMapDNA(id1)), new TransactionID(1), new ApplyTransactionInfo(), imo, false);
    mo = results.objects.get(id2);
    mo.apply(new TestMapDNA(id2), new TransactionID(2), new ApplyTransactionInfo(), imo, false);

    Map ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(2, ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(results.objects.values());

    ids.add(new ObjectID(3));
    ids.add(new ObjectID(4));
    final ObjectIDSet newIDs = new BitSetObjectIDSet();
    newIDs.add(new ObjectID(3));
    newIDs.add(new ObjectID(4));

    this.objectManager.createNewObjects(newIDs);
    results = new TestResultsContext(ids, newIDs);

    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(4, results.objects.size());

    int count = 100;
    for (final ObjectID id : ids) {
      mo = results.objects.get(id);
      if (newIDs.contains(id)) {
        mo.apply(new TestMapDNA(id), new TransactionID(count++), new ApplyTransactionInfo(), imo, false);
      } else {
        mo.apply(new TestMapDNA(id, true), new TransactionID(count++), new ApplyTransactionInfo(), imo, false);
      }
    }
    ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(4, ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(results.objects.values());
  }

  public void testLogicalFacades() throws NoSuchObjectException {
    initObjectManager();

    final ObjectID mapID = new ObjectID(1);
    final ObjectID setID = new ObjectID(3);

    final ObjectIDSet ids = new BitSetObjectIDSet();
    ids.add(mapID);
    ids.add(setID);

    this.objectManager.createNewObjects(ids);
    final TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    final ManagedObject set = lookedUpObjects.get(setID);
    final ManagedObject map = lookedUpObjects.get(mapID);

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    map.apply(new TestMapDNA(mapID), new TransactionID(1), new ApplyTransactionInfo(), imo, false);
    set.apply(new TestListSetDNA(ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName(), setID), new TransactionID(1), new ApplyTransactionInfo(), imo,
              false);

    this.objectManager.releaseAll(lookedUpObjects.values());
  }

  public void testObjectManagerBasics() {
    initObjectManager();
    final ObjectID id = new ObjectID(0);
    final ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>());
    this.objectManager.createObject(mo);
    assertFalse(this.objectManager.isReferenced(id));
    final ManagedObject mo2 = this.objectManager.getObjectByID(id);
    assertTrue(mo == mo2);
    assertTrue(this.objectManager.isReferenced(id));
    this.objectManager.release(mo);
    assertFalse(this.objectManager.isReferenced(id));

    this.objectManager.getObjectByID(id);

    final boolean[] gotIt = new boolean[1];
    gotIt[0] = false;

    final Thread t = new Thread() {
      @Override
      public void run() {
        ObjectManagerTest.this.objectManager.getObjectByID(id);
        gotIt[0] = true;
      }
    };

    t.start();
    ThreadUtil.reallySleep(1000);
    assertFalse(gotIt[0]);
    this.objectManager.release(mo);
    ThreadUtil.reallySleep(1000);
    assertTrue(gotIt[0]);
  }

  public void testObjectManagerAsync() {
    initObjectManager();
    final ObjectID id = new ObjectID(0);
    final ObjectID id1 = new ObjectID(1);

    final ObjectIDSet objectIDs = new BitSetObjectIDSet();

    final ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>());
    final ManagedObject mo1 = new TestManagedObject(id1, new ArrayList<ObjectID>());
    this.objectManager.createObject(mo);
    this.objectManager.createObject(mo1);

    assertFalse(this.objectManager.isReferenced(id));

    objectIDs.add(id);

    TestObjectManagerResultsContext context;
    assertTrue(this.objectManager
        .lookupObjectsAndSubObjectsFor(null,
                                       context = new TestObjectManagerResultsContext(
                                                                                     new HashMap<ObjectID, ManagedObject>(),
                                                                                     objectIDs), -1));

    final ManagedObject retrievedMo = (ManagedObject) context.getResults().values().iterator().next();
    assertTrue(mo == retrievedMo);
    assertTrue(this.objectManager.isReferenced(id));
    this.objectManager.release(mo);
    assertFalse(this.objectManager.isReferenced(id));

    this.objectManager.getObjectByID(id);

    objectIDs.add(id1);

    final boolean notPending = this.objectManager
        .lookupObjectsAndSubObjectsFor(null,
                                       context = new TestObjectManagerResultsContext(
                                                                                     new HashMap<ObjectID, ManagedObject>(),
                                                                                     objectIDs), -1);
    assertFalse(notPending);
    assertEquals(0, context.getResults().size());
    this.objectManager.release(mo);
    assertEquals(objectIDs.size(), context.getResults().size());

    final Collection objs = context.getResults().values();
    assertTrue(objs.contains(mo));
    assertTrue(objs.contains(mo1));
    assertTrue(objs.size() == 2);
  }

  public void testNewObjectCounter() {
    initObjectManager();
    createObjects(666);
    assertEquals(666, this.stats.getTotalObjectsCreated());
    assertEquals(666, this.newObjectCounter.getValue());

    // roots count as "new" objects too
    this.objectManager.createRoot("root", new ObjectID(4444));
    assertEquals(667, this.stats.getTotalObjectsCreated());
    assertEquals(667, this.newObjectCounter.getValue());
  }

  private ObjectIDSet makeObjectIDSet(final int begin, final int end) {
    final ObjectIDSet rv = new BitSetObjectIDSet();

    if (begin > end) {
      for (int i = begin; i > end; i--) {
        rv.add(new ObjectID(i));
      }
    } else {
      for (int i = begin; i < end; i++) {
        rv.add(new ObjectID(i));
      }
    }
    return rv;
  }

  private Set<ObjectID> createObjects(final int num) {
    return createObjects(0, num, new HashSet<ObjectID>());
  }

  public Set<ObjectID> createObjects(final int startID, final int endID, final Set<ObjectID> children) {
    return createObjects(startID, endID, children, new HashSet<TestManagedObject>());
  }

  public Set<ObjectID> createObjects(final int startID, final int endID, final Set<ObjectID> children,
                                     final Set<TestManagedObject> objects) {
    final Set<ObjectID> oidSet = new HashSet<ObjectID>(endID - startID);
    for (int i = startID; i < endID; i++) {
      final ObjectID oid = new ObjectID(i);
      oidSet.add(oid);
      final TestManagedObject mo = new TestManagedObject(oid, new ArrayList<ObjectID>(children));

      objects.add(mo);
      this.objectManager.createObject(mo);
    }
    return oidSet;
  }

  public void testObjectManagerGC() throws Exception {
    initObjectManager();
    // this should disable the gc thread.
    this.config.myGCThreadSleepTime = -1;
    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();
    final ObjectID id = new ObjectID(0);
    final ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>(3));
    this.objectManager.createObject(mo);

    assertFalse(gc.isCollected());

    gc.allow_blockUntilReadyToGC_ToProceed();

    this.objectManager.getGarbageCollector().doGC(GCType.FULL_GC);
    assertTrue(gc.isCollected());

    gc.reset();

    // call lookup to check out an object...
    this.objectManager.getObjectByID(id);

    // make sure our queues are clean
    assertFalse(gc.collectWasCalled());
    assertFalse(gc.blockUntilReadyToGC_WasCalled());

    final Thread gcCaller = new Thread(new GCCaller(), "GCCaller");
    gcCaller.start();

    // give the thread some time to start and call collect()...
    assertTrue(gc.waitForCollectToBeCalled(5000));

    // give the thread some time to call blockUntilReadyToGC()...
    assertTrue(gc.waitFor_blockUntilReadyToGC_ToBeCalled(5000));

    // ////////////////////////////////////////////////////
    // now call release and make sure it calls the appropriate GC methods...

    assertFalse(gc.notifyReadyToGC_WasCalled());
    this.objectManager.release(mo);

    // make sure release calls notifyReadyToGC
    assertTrue(gc.waitFor_notifyReadyToGC_ToBeCalled(5000));

    // unblock the caller...
    gc.allow_blockUntilReadyToGC_ToProceed();

    // make sure the object manager calls notifyGCComplete
    assertTrue(gc.waitFor_notifyGCComplete_ToBeCalled(5000));
    gcCaller.join();
  }

  public void testGetObjectReferencesFrom() {
    this.config.paranoid = true;
    initObjectManager();

    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    // each object has 1000 distinct reachable objects
    final Set<ObjectID> children = createObjects(1000, 2000, new HashSet<ObjectID>());

    createObjects(0, 2, children);
    final Set<TestManagedObject> objects = new HashSet<TestManagedObject>();

    createObjects(3, 5, children, objects);
    final Set<ObjectID> returnedSet = this.objectManager.getObjectReferencesFrom(new ObjectID(1), false);
    assertEquals(1000, returnedSet.size());

    // ObjectManager should give out objects references counts to DGC even if its passed
    gc.requestGCPause();

    final Set<ObjectID> returnedCachedNoReapSet = this.objectManager.getObjectReferencesFrom(new ObjectID(4), true);
    assertEquals(1000, returnedCachedNoReapSet.size());

    gc.notifyReadyToGC();
    gc.notifyGCComplete();
  }

  public void testFaultWithConcurrentRemove() throws Exception {
    this.config.paranoid = true;
    initObjectManager();
    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    final Set<ObjectID> oids = createObjects(100);
    final AtomicBoolean fail = new AtomicBoolean(true);
    final CyclicBarrier barrier = new CyclicBarrier(2);

    final Thread faulter = new Thread("testFaultWithConcurrentRemove-faulter") {
      @Override
      public void run() {
        try {
          for (ObjectID oid : oids) {
            barrier.await(5, TimeUnit.SECONDS);
            logger.info("Looking up " + oid);
            ManagedObject mo = objectManager.getObjectByIDReadOnly(oid);
            if (mo != null) {
              // Only need to release if we actually got it, since we're intentionally racing with the remove below,
              // it's possible that the ManagedObject was already gone before we got it.
              objectManager.releaseReadOnly(mo);
            }
          }
          fail.set(false);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    faulter.setDaemon(true);

    final Thread remover = new Thread("testFaultWithConcurrentRemove-remover") {
      @Override
      public void run() {
        try {
          for (ObjectID oid : oids) {
            logger.info("Deleting " + oid);
            DGCResultContext dgcResultContext = new DGCResultContext(new BitSetObjectIDSet(Collections.singleton(oid)), null);
            barrier.await(5, TimeUnit.SECONDS);
            objectManager.notifyGCComplete(dgcResultContext);
            objectManager.getGarbageCollector().notifyGCComplete();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    remover.setDaemon(true);

    faulter.start();
    remover.start();

    // Setup a timeout of 5 minutes so this test doesn't wind up taking forever, both the remover and faulter threads
    // end up blocking forever if any error occurs, so we need this so we don't wait for 29 minutes if the test fails.
    Timer watchdog = new Timer();
    watchdog.schedule(new TimerTask() {
      @Override
      public void run() {
        faulter.interrupt();
        remover.interrupt();
      }
    }, 300 * 1000);

    faulter.join();
    watchdog.cancel();
    // Don't wait up for the remover, there's an infinite loop if the faulter thread fails
    assertFalse(fail.get());
  }

  public void testConcurrentLookupRemoveOnReleaseEntryTest() throws Exception {
    final ObjectID oid = new ObjectID(1);
    initObjectManager();

    ManagedObject mo = objectStore.createObject(oid);
    mo.apply(new TestSerialziedEntryDNA(oid), new TransactionID(1), new ApplyTransactionInfo(), mock(ObjectInstanceMonitor.class), true);

    Assert.assertNotNull(mo = objectManager.getObjectByID(oid));
    objectManager.releaseReadOnly(mo);

    ExecutorService executorService = Executors.newCachedThreadPool();
    List<Callable<Void>> runnables = new ArrayList<Callable<Void>>(5);
    for (int i = 0; i < 5; i++) {
      runnables.add(new Callable<Void>() {
        @Override
        public Void call() {
          for (int j = 0; j < 1000; j++) {
            ManagedObject moInternal = objectManager.getObjectByID(oid);
            Assert.assertNotNull(moInternal);
            objectManager.releaseReadOnly(moInternal);
          }
          return null;
        }
      });
    }

    for (Future<?> future : executorService.invokeAll(runnables)) {
      future.get();
    }

    executorService.shutdown();
  }

  public void testDeleteNewObject() throws Exception {
    final ObjectID id = new ObjectID(1);
    initObjectManager();
    objectManager.createNewObjects(Collections.singleton(id));

    ExecutorService executorService = Executors.newCachedThreadPool();

    Future<?> f = executorService.submit(new Runnable() {
      @Override
      public void run() {
        objectManager.deleteObjects(Collections.singleton(id));
      }
    });

    try {
      f.get(2, TimeUnit.SECONDS);
      fail();
    } catch (TimeoutException e) {
      // Expected, since the object is new, it'll block the delete.
    }

    ObjectIDSet oids = new BitSetObjectIDSet(Collections.singleton(id));
    TestResultsContext context = new TestResultsContext(oids, oids);
    objectManager.lookupObjectsFor(new ClientID(1), context);
    context.waitTillComplete();
    Assert.assertNotNull(context.objects.get(id));

    objectManager.release(context.objects.get(id));

    f.get();
  }

  private static class TestSerialziedEntryDNA implements DNA {
    private final ObjectID oid;

    private TestSerialziedEntryDNA(final ObjectID oid) {
      this.oid = oid;
    }

    @Override
    public long getVersion() {
      return 0L;
    }

    @Override
    public boolean hasLength() {
      return false;
    }

    @Override
    public int getArraySize() {
      return 0;
    }

    @Override
    public boolean isDelta() {
      return false;
    }

    @Override
    public String getTypeName() {
      return ManagedObjectStateStaticConfig.SERIALIZED_MAP_VALUE.getClientClassName();
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return oid;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    @Override
    public DNACursor getCursor() {
      TestDNACursor cursor = new TestDNACursor();
      cursor.addEntireArray(new byte[1]);
      return cursor;
    }
  }

  private static class TestListSetDNA implements DNA {

    final ObjectID setID;
    final String   className;

    public TestListSetDNA(final String className, final ObjectID setID) {
      this.className = className;
      this.setID = setID;
    }

    @Override
    public long getVersion() {
      return 0;
    }

    @Override
    public boolean hasLength() {
      return false;
    }

    @Override
    public int getArraySize() {
      return -1;
    }

    @Override
    public String getTypeName() {
      return this.className;
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return this.setID;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    @Override
    public DNACursor getCursor() {
      return new DNACursor() {
        int count;

        @Override
        public boolean next() {
          this.count++;
          return this.count <= 3;
        }

        @Override
        public LogicalAction getLogicalAction() {
          return (LogicalAction)getAction();
        }

        @Override
        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        @Override
        public boolean next(final DNAEncoding encoding) {
          throw new ImplementMe();
        }

        @Override
        public Object getAction() {
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              final Object item = new UTF8ByteDataHolder("item" + this.count);
              return new LogicalAction(LogicalOperation.ADD, new Object[] { item });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        @Override
        public int getActionCount() {
          return 3;
        }

        @Override
        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    @Override
    public boolean isDelta() {
      return false;
    }
  }

  private static class TestMapDNA implements DNA {

    private static final AtomicLong versionCounter = new AtomicLong();
    final ObjectID        objectID;
    private final boolean isDelta;
    private final long version;

    TestMapDNA(final ObjectID id) {
      this(id, false);
    }

    TestMapDNA(final ObjectID id, final boolean isDelta) {
      this.objectID = id;
      this.isDelta = isDelta;
      this.version = versionCounter.incrementAndGet();
    }

    @Override
    public long getVersion() {
      return version;
    }

    @Override
    public boolean hasLength() {
      return false;
    }

    @Override
    public int getArraySize() {
      return -1;
    }

    @Override
    public String getTypeName() {
      return "com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl";
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return this.objectID;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    @Override
    public DNACursor getCursor() {
      return new DNACursor() {

        int count = 0;

        @Override
        public boolean next() {
          this.count++;
          return this.count <= 3;
        }

        @Override
        public LogicalAction getLogicalAction() {
          return (LogicalAction)getAction();
        }

        @Override
        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        @Override
        public boolean next(final DNAEncoding encoding) {
          throw new ImplementMe();
        }

        @Override
        public Object getAction() {
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              final Object key = new UTF8ByteDataHolder("key" + this.count);
              final Object val = new UTF8ByteDataHolder("val" + this.count);
              return new LogicalAction(LogicalOperation.PUT, new Object[] { key, val });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        @Override
        public int getActionCount() {
          return 3;
        }

        @Override
        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    @Override
    public boolean isDelta() {
      return this.isDelta;
    }
  }

  private static class TestResultsContext implements ObjectManagerResultsContext {
    public Map<ObjectID, ManagedObject> objects  = new HashMap<ObjectID, ManagedObject>();
    public Set<ObjectID>                missing  = new HashSet<ObjectID>();
    boolean                             complete = false;
    private final ObjectIDSet ids;
    private final ObjectIDSet newIDS;

    public TestResultsContext(final ObjectIDSet ids, final ObjectIDSet newIDS) {
      this.ids = ids;
      this.newIDS = newIDS;
    }

    public synchronized void waitTillComplete() {
      while (!this.complete) {
        try {
          wait();
        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }

    @Override
    public synchronized void setResults(final ObjectManagerLookupResults results) {
      this.complete = true;
      this.objects.putAll(results.getObjects());
      this.missing.addAll(results.getMissingObjectIDs());
      // if (!results.getMissingObjectIDs().isEmpty()) { throw new AssertionError("Missing Object : "
      // + results.getMissingObjectIDs()); }
      notifyAll();
    }

    @Override
    public ObjectIDSet getLookupIDs() {
      return this.ids;
    }

    @Override
    public ObjectIDSet getNewObjectIDs() {
      return this.newIDS;
    }
  }

  private class GCCaller implements Runnable {

    @Override
    public void run() {
      ObjectManagerTest.this.objectManager.getGarbageCollector().doGC(GCType.FULL_GC);
    }
  }

  /*
   * @see TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static class TestObjectManagerConfig extends ObjectManagerConfig {

    public long    myGCThreadSleepTime = 100;
    public boolean paranoid;

    public TestObjectManagerConfig() {
      super(10000, true, true, true);
    }

    @Override
    public long gcThreadSleepTime() {
      return this.myGCThreadSleepTime;
    }

    @Override
    public boolean paranoid() {
      return this.paranoid;
    }
  }
}
