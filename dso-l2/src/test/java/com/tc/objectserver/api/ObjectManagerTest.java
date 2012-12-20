/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import org.mockito.Mockito;

import com.tc.exception.ImplementMe;
import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.DGCResultContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
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
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.persistence.HeapStorageManagerFactory;
import com.tc.objectserver.persistence.Persistor;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.ServerTransactionSequencer;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.TestServerTransactionManager;
import com.tc.objectserver.tx.TestTransactionalStageCoordinator;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;

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
  private TestPersistenceTransactionProvider persistenceTransactionProvider;
  private TestTransactionalStageCoordinator  coordinator;
  private TransactionalObjectManagerImpl     txObjectManager;
  private long                               version = 0;
  private Persistor persistor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.persistor = new Persistor(HeapStorageManagerFactory.INSTANCE);
    persistor.start();
    this.logger = TCLogging.getLogger(getClass());
    this.config = new TestObjectManagerConfig();
    this.clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);
    this.newObjectCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.stats = new ObjectManagerStatsImpl(this.newObjectCounter);
    this.persistenceTransactionProvider = new TestPersistenceTransactionProvider();
  }

  private void initObjectManager() {
    this.objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    initObjectManager(this.objectStore);
  }

  private void initObjectManager(final PersistentManagedObjectStore store) {
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store,
                                               this.persistenceTransactionProvider);
  }

  private void initTransactionObjectManager() {
    final ServerTransactionSequencer sequencer = new ServerTransactionSequencerImpl();
    this.coordinator = new TestTransactionalStageCoordinator();
    this.txObjectManager = new TransactionalObjectManagerImpl(this.objectManager, sequencer, new TestGlobalTransactionManager(),
                                                              this.coordinator);
    ServerConfigurationContext scc = Mockito.mock(ServerConfigurationContext.class);
    Mockito.when(scc.getTransactionManager()).thenReturn(new TestServerTransactionManager());
    this.objectManager.setTransactionalObjectManager(this.txObjectManager);
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
    this.objectManager.setStatsListener(this.stats);

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
    TestResultsContext results = new TestResultsContext(ids, new ObjectIDSet(), true);

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
    results = new TestResultsContext(ids, new ObjectIDSet(), true);

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
    final TestResultsContext result1 = new TestResultsContext(ids, new ObjectIDSet(), true);

    this.objectManager.lookupObjectsAndSubObjectsFor(null, result1, -1);
    result1.waitTillComplete();

    // Now look two missing objects
    final ObjectIDSet missingids = makeObjectIDSet(20, 22);
    final TestResultsContext result2 = new TestResultsContext(missingids, new ObjectIDSet(), true);

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

    final ObjectIDSet ids = new ObjectIDSet(); // important to use a Set here

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
    final ObjectIDSet newIDs = new ObjectIDSet();
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

    final ObjectIDSet ids = new ObjectIDSet();
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

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(mapID, -1);
    validateMapFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(mapID, 5);
    validateMapFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(mapID, 1);
    validateMapFacade(facade, 1, 3);
    facade = this.objectManager.lookupFacade(mapID, 0);
    validateMapFacade(facade, 0, 3);

    facade = this.objectManager.lookupFacade(setID, -1);
    validateListFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(setID, 5);
    validateListFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(setID, 1);
    validateListFacade(facade, 1, 3);
    facade = this.objectManager.lookupFacade(setID, 0);
    validateListFacade(facade, 0, 3);
  }

  private void validateListFacade(final ManagedObjectFacade setFacade, final int facadeSize, final int totalSize) {
    assertFalse(setFacade.isArray());
    assertFalse(setFacade.isMap());
    assertTrue(setFacade.isList());
    assertEquals(ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName(), setFacade.getClassName());
    assertEquals(facadeSize, setFacade.getFacadeSize());
    assertEquals(totalSize, setFacade.getTrueObjectSize());

    final List<String> expect = new ArrayList<String>();
    expect.add("item1");
    expect.add("item2");
    expect.add("item3");

    final List<String> actual = new ArrayList<String>();
    for (int i = 0; i < facadeSize; i++) {
      final String fName = String.valueOf(i);
      final Object value = setFacade.getFieldValue(fName);
      assertTrue(value instanceof String);
      actual.add((String) value);
    }

    assertTrue(expect.containsAll(actual));
  }

  private void validateMapFacade(final ManagedObjectFacade mapFacade, final int facadeSize, final int totalSize) {
    assertFalse(mapFacade.isArray());
    assertTrue(mapFacade.isMap());
    assertFalse(mapFacade.isSet());
    assertFalse(mapFacade.isList());
    assertEquals("com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl", mapFacade.getClassName());
    assertEquals(facadeSize, mapFacade.getFacadeSize());
    assertEquals(totalSize, mapFacade.getTrueObjectSize());

    final Map<String, String> expect = new HashMap<String, String>();
    expect.put("key1", "val1");
    expect.put("key2", "val2");
    expect.put("key3", "val3");

    final Map<String, String> actual = new HashMap<String, String>();

    for (int i = 0; i < facadeSize; i++) {
      final String fName = String.valueOf(i);
      final Object value = mapFacade.getFieldValue(fName);
      assertTrue(value instanceof MapEntryFacade);
      final MapEntryFacade entry = (MapEntryFacade) value;
      actual.put((String) entry.getKey(), (String) entry.getValue());
    }

    for (final String key : actual.keySet()) {
      assertEquals(expect.get(key), actual.get(key));
    }
  }

  private static void close(final Persistor persistor, final PersistentManagedObjectStore store) {
    // to work around timing problem with this test, calling snapshot
    // this should block this thread until transaction reading all object IDs from BDB completes,
    // at which point, it's OK to close the DB
    persistor.getManagedObjectPersistor().snapshotObjectIDs();
    persistor.getManagedObjectPersistor().snapshotEvictableObjectIDs();
//    persistor.getManagedObjectPersistor().snapshotMapTypeObjectIDs();
    try {
      store.shutdown();
      persistor.close();
    } catch (Throwable e) {
      System.err.println("\n### Error closing resources: " + e);
      e = e.getCause();
      while (e != null) {
        System.err.println("\n### Caused by: " + e);
        e = e.getCause();
      }

    }
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

    final ObjectIDSet objectIDs = new ObjectIDSet();

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
    this.objectManager.setStatsListener(this.stats);
    createObjects(666);
    assertEquals(666, this.stats.getTotalObjectsCreated());
    assertEquals(666, this.newObjectCounter.getValue());

    // roots count as "new" objects too
    this.objectManager.createRoot("root", new ObjectID(4444));
    assertEquals(667, this.stats.getTotalObjectsCreated());
    assertEquals(667, this.newObjectCounter.getValue());
  }

  private ObjectIDSet makeObjectIDSet(final int begin, final int end) {
    final ObjectIDSet rv = new ObjectIDSet();

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

  public void testLookupFacadeForMissingObject() {
    initObjectManager();

    try {
      this.objectManager.lookupFacade(new ObjectID(1), -1);
      fail("lookup didn't throw exception");
    } catch (final NoSuchObjectException e) {
      // expected
    }
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

  /**
   * This test is written to expose an case which used to trigger an assertion error in ObjectManager when GC initiates
   * recall in TransactionalObjectManager in persistence mode
   */
  public void testRecallNewObjects() throws Exception {
    final PersistentManagedObjectStore persistentMOStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    this.objectStore = persistentMOStore;
    this.config.paranoid = true;
    initObjectManager(this.objectStore);
    initTransactionObjectManager();

    // this should disable the gc thread.
    this.config.myGCThreadSleepTime = -1;
    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    /**
     * STEP 1: Create an New object and check it out
     */
    final Map<ObjectID, DNA> changes = new HashMap<ObjectID, DNA>();

    final String fieldName = "whatsup";
    final AtomicReference<ObjectID> atomicReference = new AtomicReference<ObjectID>(new ObjectID(500));

    changes.put(new ObjectID(1), new TestServerMapDNA(new ObjectID(1), fieldName, atomicReference));

    final ServerTransaction stxn1 = new ServerTransactionImpl(new TxnBatchID(1), new TransactionID(1),
                                                              new SequenceID(1), new LockID[0], new ClientID(2),
                                                              new ArrayList<DNA>(changes.values()),
                                                              new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                              TxnType.NORMAL, new LinkedList(),
                                                              DmiDescriptor.EMPTY_ARRAY, new MetaDataReader[0], 1,
                                                              new long[0]);

    final List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
    txns.add(stxn1);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    LookupEventContext loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.txObjectManager.lookupObjectsForTransactions();

    // Apply should have been called as we have Object 1
    ApplyTransactionContext aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn1 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // Apply and initate commit the txn
    ApplyTransactionInfo applyTxnInfo1 = applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(applyTxnInfo1);

    /**
     * STEP 2: Don't check back Object 1 yet, make another transaction with yet another object
     */
    changes.clear();

    final String fieldName2 = "whatsup2";
    final AtomicReference<ObjectID> atomicReference2 = new AtomicReference<ObjectID>(new ObjectID(501));
    changes.put(new ObjectID(2), new TestServerMapDNA(new ObjectID(2), fieldName2, atomicReference2));

    final ServerTransaction stxn2 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2),
                                                              new SequenceID(1), new LockID[0], new ClientID(2),
                                                              new ArrayList<DNA>(changes.values()),
                                                              new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                              TxnType.NORMAL, new LinkedList(),
                                                              DmiDescriptor.EMPTY_ARRAY, new MetaDataReader[0], 1,
                                                              new long[0]);

    txns.clear();
    txns.add(stxn2);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.txObjectManager.lookupObjectsForTransactions();

    // Apply should have been called as we have Object 2
    aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn2 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    /**
     * STEP 3: Create a txn with Objects 1,2 and a new object 3
     */
    changes.clear();

    final String fieldName3 = "whatsup3";
    final AtomicReference<ObjectID> atomicReference3 = new AtomicReference<ObjectID>(new ObjectID(505));

    changes.put(new ObjectID(1), new TestServerMapDNA(new ObjectID(1), true, fieldName3, atomicReference3));
    changes.put(new ObjectID(2), new TestServerMapDNA(new ObjectID(2), true, fieldName3, atomicReference3));
    changes.put(new ObjectID(3), new TestServerMapDNA(new ObjectID(3), fieldName3, atomicReference3));

    final ServerTransaction stxn3 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2),
                                                              new SequenceID(1), new LockID[0], new ClientID(2),
                                                              new ArrayList<DNA>(changes.values()),
                                                              new ObjectStringSerializerImpl(), Collections.EMPTY_MAP,
                                                              TxnType.NORMAL, new LinkedList(),
                                                              DmiDescriptor.EMPTY_ARRAY, new MetaDataReader[0], 1,
                                                              new long[0]);

    txns.clear();
    txns.add(stxn3);

    // createNewObjectFromTransaction(txns);

    this.txObjectManager.addTransactions(txns);

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    // This lookup should go pending since we don't have Object 1, since 2 is already checkedout only 1,3 should be
    // requested.
    this.txObjectManager.lookupObjectsForTransactions();

    // Apply should not have been called as we don't have Object 1
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    /**
     * STEP 4: Commit but not release Object 2 so even when we check object 1 back stxn3 is still pending
     */
    // Apply and initiate commit the txn for object 2
    ApplyTransactionInfo applyTxnInfo2 = applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(applyTxnInfo2);

    /**
     * STEP 4: Check in Object 1 thus releasing the blocked lookup for Object 1, 3
     */

    // Now check back Object 1
    this.objectManager.releaseAll(applyTxnInfo1.getObjectsToRelease());

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    /**
     * STEP 5 : Before lookup is initiated, initiate a GC pause
     */
    gc.requestGCPause();
    // Doing in a separate thread since this will block
    final CyclicBarrier cb = new CyclicBarrier(2);
    final Thread t = new Thread("GC Thread - testRecallNewObjects") {
      @Override
      public void run() {
        ObjectManagerTest.this.objectManager.waitUntilReadyToGC();
        try {
          cb.await();
        } catch (final Exception e) {
          e.printStackTrace();
          throw new AssertionError(e);
        }
      }
    };
    t.start();
    ThreadUtil.reallySleep(5000);

    // Recall request should have be added.
    final RecallObjectsContext roc = (RecallObjectsContext) this.coordinator.recallSink.queue.take();
    assertNotNull(roc);
    assertTrue(this.coordinator.recallSink.queue.isEmpty());

    assertTrue(roc.recallAll());

    // do recall - This used to cause an assertion error in persistent mode
    this.txObjectManager.recallCheckedoutObject(roc);

    // Check in Object 2 to make the GC go to paused state
    this.objectManager.releaseAll(applyTxnInfo2.getObjectsToRelease());

    cb.await();

    assertTrue(gc.isPaused());

    // Complete gc
    gc.deleteGarbage(new DGCResultContext(TCCollections.EMPTY_OBJECT_ID_SET, new GarbageCollectionInfo()));

    // Lookup context should have been fired
    loc = (LookupEventContext) this.coordinator.lookupSink.queue.take();
    assertNotNull(loc);
    assertTrue(this.coordinator.lookupSink.queue.isEmpty());

    this.txObjectManager.lookupObjectsForTransactions();

    // Apply should have been called for txn 3
    aoc = (ApplyTransactionContext) this.coordinator.applySink.queue.take();
    assertTrue(stxn3 == aoc.getTxn());
    assertNotNull(aoc);
    assertTrue(this.coordinator.applySink.queue.isEmpty());

    // Apply and initiate commit the txn
    ApplyTransactionInfo applyTxnInfo3 = applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(applyTxnInfo3);

    // Now check back the objects
    this.objectManager.releaseAll(applyTxnInfo3.getObjectsToRelease());

    assertEquals(0, this.objectManager.getCheckedOutCount());
    assertFalse(this.objectManager.isReferenced(new ObjectID(1)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(2)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(3)));

    close(persistor, persistentMOStore);
  }

  public void testGetObjectReferencesFrom() {
    this.config.paranoid = true;
    initObjectManager();
    this.objectManager.setStatsListener(this.stats);

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
            DGCResultContext dgcResultContext = new DGCResultContext(new ObjectIDSet(Collections.singleton(oid)), null);
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

  private ApplyTransactionInfo applyTxn(final ApplyTransactionContext aoc) {
    final ServerTransaction txn = aoc.getTxn();
    final Map managedObjects = aoc.getObjects();
    final ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    final ApplyTransactionInfo applyTxnInfo = new ApplyTransactionInfo(txn.isActiveTxn(), txn.getServerTransactionID(),
                                                                       false);
    for (final Object o : txn.getChanges()) {
      final DNA dna = (DNA)o;
      final ManagedObject mo = (ManagedObject)managedObjects.get(dna.getObjectID());
      mo.apply(new VersionizedDNAWrapper(dna, ++this.version), txn.getTransactionID(), applyTxnInfo, instanceMonitor,
          false);
    }
    return applyTxnInfo;
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
      cursor.addPhysicalAction("value", new byte[1], false);
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
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              final Object item = new UTF8ByteDataHolder("item" + this.count);
              return new LogicalAction(SerializationUtil.ADD, new Object[] { item });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
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
          throw new ImplementMe();
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
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              final Object key = new UTF8ByteDataHolder("key" + this.count);
              final Object val = new UTF8ByteDataHolder("val" + this.count);
              return new LogicalAction(SerializationUtil.PUT, new Object[] { key, val });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
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
          throw new ImplementMe();
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

  public static class TestDateDNA implements DNA {

    final ObjectID setID;
    final String   className;

    public TestDateDNA(final String className, final ObjectID setID) {
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
          return this.count <= 1;
        }

        @Override
        public LogicalAction getLogicalAction() {
          switch (this.count) {
            case 1:
              return new LogicalAction(SerializationUtil.SET_TIME, new Object[] { System.currentTimeMillis() });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
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
          throw new ImplementMe();
        }

        @Override
        public int getActionCount() {
          return 1;
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

  private static class TestResultsContext implements ObjectManagerResultsContext {
    public Map<ObjectID, ManagedObject> objects  = new HashMap<ObjectID, ManagedObject>();
    public Set<ObjectID>                missing  = new HashSet<ObjectID>();
    boolean                             complete = false;
    private final ObjectIDSet           ids;
    private final ObjectIDSet           newIDS;

    public TestResultsContext(final ObjectIDSet ids, final ObjectIDSet newIDS, final boolean updateStats) {
      this.ids = ids;
      this.newIDS = newIDS;
    }

    public TestResultsContext(final ObjectIDSet ids, final ObjectIDSet newIDS) {
      this(ids, newIDS, true);
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
      super(10000, true, true, true, false, 60000, 1000);
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

  private static final class TestServerMapDNA implements DNA {
    private final ObjectID                  id;
    private final boolean                   isDelta;
    private final String                    fieldName;
    private final AtomicReference<ObjectID> oidHolder;

    public TestServerMapDNA(final ObjectID id, String fieldName, AtomicReference<ObjectID> oidHolder) {
      this(id, false, fieldName, oidHolder);
    }

    public TestServerMapDNA(final ObjectID id, final boolean isDelta, String fieldName,
                            AtomicReference<ObjectID> oidHolder) {
      this.isDelta = isDelta;
      this.id = id;
      this.fieldName = fieldName;
      this.oidHolder = oidHolder;
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
    public boolean isDelta() {
      return isDelta;
    }

    @Override
    public String getTypeName() {
      return "com.terracotta.toolkit.roots.impl.ToolkitTypeRootImpl";
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return this.id;
    }

    @Override
    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    @Override
    public DNACursor getCursor() {
      if (fieldName == null) { return new TestMapCursor(); }
      return new TestMapCursor(fieldName, oidHolder);
    }

  }

  private static final class TestMapCursor implements DNACursor {
    private final String                    fieldName;
    private final AtomicReference<ObjectID> oidHolder;
    private volatile boolean                hasNext = true;

    public TestMapCursor() {
      this(null, null);
      hasNext = false;
    }

    public TestMapCursor(String fieldName, AtomicReference<ObjectID> oidHolder) {
      this.fieldName = fieldName;
      this.oidHolder = oidHolder;
    }

    @Override
    public LogicalAction getLogicalAction() {
      return new LogicalAction(SerializationUtil.PUT, new Object[] { fieldName, oidHolder.get() });
    }

    @Override
    public PhysicalAction getPhysicalAction() {
      return null;
    }

    @Override
    public boolean next() {
      if (hasNext) {
        hasNext = false;
        return true;
      }
      return false;
    }

    @Override
    public boolean next(final DNAEncoding encoding) {
      throw new ImplementMe();
    }

    @Override
    public Object getAction() {
      throw new ImplementMe();
    }

    @Override
    public int getActionCount() {
      return 1;
    }

    @Override
    public void reset() throws UnsupportedOperationException {
      hasNext = true;
    }
  }
}
