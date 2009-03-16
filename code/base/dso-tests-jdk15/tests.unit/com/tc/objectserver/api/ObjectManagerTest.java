/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.async.impl.MockSink;
import com.tc.exception.ImplementMe;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.LRUEvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.cache.TestCacheStats;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.ApplyCompleteEventContext;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.GCResultContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.dgc.api.GarbageCollectionInfo;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;
import com.tc.objectserver.gtx.TestGlobalTransactionManager;
import com.tc.objectserver.impl.InMemoryManagedObjectStore;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.impl.ObjectManagerStatsImpl;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.TestTransactionalStageCoordinator;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

/**
 * @author steve
 */
public class ObjectManagerTest extends BaseDSOTestCase {

  private Map                                managed;
  private ObjectManagerImpl                  objectManager;
  private TestObjectManagerConfig            config;
  private ClientStateManager                 clientStateManager;
  private ManagedObjectStore                 objectStore;
  private TCLogger                           logger;
  private ObjectManagerStatsImpl             stats;
  private SampledCounter                     newObjectCounter;
  private SampledCounterImpl                 objectfaultCounter;
  private TestPersistenceTransactionProvider persistenceTransactionProvider;
  private TestPersistenceTransaction         NULL_TRANSACTION;
  private TestTransactionalStageCoordinator  coordinator;
  private TestGlobalTransactionManager       gtxMgr;
  private TransactionalObjectManagerImpl     txObjectManager;
  private TestSinkContext                    testFaultSinkContext;
  private long                               version = 0;
  private ObjectStatsRecorder                objectStatsRecorder;

  /**
   * Constructor for ObjectManagerTest.
   * 
   * @param arg0
   */
  public ObjectManagerTest(String arg0) {
    super(arg0);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.logger = TCLogging.getLogger(getClass());
    this.managed = new HashMap();
    this.config = new TestObjectManagerConfig();
    this.clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());
    this.newObjectCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.objectfaultCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.stats = new ObjectManagerStatsImpl(this.newObjectCounter, this.objectfaultCounter);
    this.persistenceTransactionProvider = new TestPersistenceTransactionProvider();
    this.NULL_TRANSACTION = TestPersistenceTransaction.NULL_TRANSACTION;
    this.objectStatsRecorder = new ObjectStatsRecorder();
  }

  private void initObjectManager() {
    initObjectManager(createThreadGroup());
  }

  private TCThreadGroup createThreadGroup() {
    return new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(ObjectManagerImpl.class)));
  }

  private void initObjectManager(ThreadGroup threadGroup) {
    initObjectManager(threadGroup, new NullCache());
  }

  private void initObjectManager(ThreadGroup threadGroup, EvictionPolicy cache) {
    this.objectStore = new InMemoryManagedObjectStore(this.managed);
    initObjectManager(threadGroup, cache, this.objectStore);
  }

  private void initObjectManager(ThreadGroup threadGroup, EvictionPolicy cache, ManagedObjectStore store) {
    TestSink faultSink = new TestSink();
    TestSink flushSink = new TestSink();
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store, cache,
                                               this.persistenceTransactionProvider, faultSink, flushSink,
                                               this.objectStatsRecorder);
    this.testFaultSinkContext = new TestSinkContext();
    new TestMOFaulter(this.objectManager, store, faultSink, this.testFaultSinkContext).start();
    new TestMOFlusher(this.objectManager, flushSink, new NullSinkContext()).start();
  }

  private void initTransactionObjectManager() {
    ServerTransactionSequencerImpl sequencer = new ServerTransactionSequencerImpl();
    this.coordinator = new TestTransactionalStageCoordinator();
    this.gtxMgr = new TestGlobalTransactionManager();
    this.txObjectManager = new TransactionalObjectManagerImpl(this.objectManager, sequencer, this.gtxMgr,
                                                              this.coordinator);
    this.objectManager.setTransactionalObjectManager(this.txObjectManager);
  }

  public void testShutdownAndSetGarbageCollector() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.setGarbageCollector(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookup() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.getObjectByID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok;
    }
  }

  public void testShutdownAndLookupRootID() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.lookupRootID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateRoot() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.createRoot(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateObject() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.createObject(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndGetRoots() throws Exception {
    initObjectManager();
    this.objectManager.stop();
    try {
      this.objectManager.getRoots();
      fail("Should have thrown a ShutdownError");
    } catch (ShutdownError e) {
      // ok.
    }

  }

  public void testShutdownAndLookupObjectsForCreateIfNecessary() throws Exception {
    initObjectManager();

    this.objectManager.stop();

    try {
      this.objectManager.lookupObjectsFor(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookupObjectsFor() throws Exception {
    initObjectManager();

    this.objectManager.stop();

    try {
      this.objectManager.lookupObjectsAndSubObjectsFor(null, null, -1);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testPreFetchObjects() {
    this.config.paranoid = true;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))),
                      new LRUEvictionPolicy(-1));
    this.objectManager.setStatsListener(this.stats);

    // first assert that no hits/misses occurred for clean stats.
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(0, this.stats.getTotalCacheMisses());

    // create your initial objects
    createObjects(50, 10);

    // CASE 1: no preFetched objects
    ObjectIDSet ids = makeObjectIDSet(0, 10);
    TestResultsContext results = new TestResultsContext(ids, new ObjectIDSet(), true);
    this.testFaultSinkContext.resetCounter();
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    this.testFaultSinkContext.waitUntillCounterIs(10);
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());

    // before no objects were pre-fetched, we should expect 0 hits and 10 misses
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(10, this.stats.getTotalCacheMisses());

    // CASE 2: preFetched objects
    ids = makeObjectIDSet(10, 20);
    this.testFaultSinkContext.resetCounter();
    this.objectManager.preFetchObjectsAndCreate(ids, Collections.<ObjectID> emptySet());
    this.testFaultSinkContext.waitUntillCounterIs(10);

    // because objects where prefetched we should have 10 hits, but also 10 moreT
    // misses because the prefetching gets factored in as a miss to bring the total
    // to 20
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(20, this.stats.getTotalCacheMisses());

    this.testFaultSinkContext.resetCounter();
    results = new TestResultsContext(ids, new ObjectIDSet(), false);
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    assertEquals(0, this.testFaultSinkContext.getCounter());
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());

    // because objects where prefetched we should have 10 hits, but also 10 more
    // misses because the prefetching gets factored in as a miss to bring the total
    // to 20
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(20, this.stats.getTotalCacheMisses());

  }

  public void testMissingObjects() {

    initObjectManager();

    createObjects(10, 10);

    // Look up two existing objects
    ObjectIDSet ids = makeObjectIDSet(1, 2);
    TestResultsContext result1 = new TestResultsContext(ids, new ObjectIDSet(), true);

    this.testFaultSinkContext.resetCounter();
    this.objectManager.lookupObjectsAndSubObjectsFor(null, result1, -1);
    result1.waitTillComplete();
    assertEquals(0, this.testFaultSinkContext.getCounter());

    // Now look two missing objects
    ObjectIDSet missingids = makeObjectIDSet(20, 22);
    TestResultsContext result2 = new TestResultsContext(missingids, new ObjectIDSet(), true);

    this.testFaultSinkContext.resetCounter();
    this.objectManager.lookupObjectsAndSubObjectsFor(null, result2, -1);
    this.testFaultSinkContext.waitUntillCounterIs(2);
    assertEquals(missingids, result2.missing);

    // Now release the first two objects
    this.objectManager.releaseAll(this.NULL_TRANSACTION, result1.objects.values());

    // Counter shouldn't be incremented, in other words, missing objects should not be looked up again.
    assertEquals(2, this.testFaultSinkContext.getCounter());
  }

  public void testNewObjectIDs() {
    // this test is to make sure that the list of newly created objects IDs is
    // accurate in the lookup results
    initObjectManager();

    ObjectIDSet ids = new ObjectIDSet(); // important to use a Set here

    ObjectID id1;
    ids.add((id1 = new ObjectID(1)));
    ObjectID id2;
    ids.add((id2 = new ObjectID(2)));
    ClientID key = new ClientID(0);

    this.objectManager.createNewObjects(ids);

    TestResultsContext results = new TestResultsContext(ids, ids);
    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(2, results.objects.size());

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();

    ManagedObject mo = results.objects.get(id1);
    TestArrayDNA ta;
    mo.apply((ta = new TestArrayDNA(id1)), new TransactionID(1), new BackReferences(), imo, false);
    mo = results.objects.get(id2);
    mo.apply(new TestArrayDNA(id2), new TransactionID(2), new BackReferences(), imo, false);

    Map ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(2, ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());

    ids.add(new ObjectID(3));
    ids.add(new ObjectID(4));
    ObjectIDSet newIDs = new ObjectIDSet();
    newIDs.add(new ObjectID(3));
    newIDs.add(new ObjectID(4));

    this.objectManager.createNewObjects(newIDs);
    results = new TestResultsContext(ids, newIDs);

    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(4, results.objects.size());

    int count = 100;
    for (Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      ObjectID id = iter.next();
      mo = results.objects.get(id);
      if (newIDs.contains(id)) {
        mo.apply(new TestArrayDNA(id), new TransactionID(count++), new BackReferences(), imo, false);
      } else {
        mo.apply(new TestArrayDNA(id, true), new TransactionID(count++), new BackReferences(), imo, false);

      }
    }
    ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(4, ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());
  }

  public void testArrayFacade() throws Exception {
    initObjectManager();

    ObjectID id = new ObjectID(1);
    ObjectIDSet ids = new ObjectIDSet();
    ids.add(id);
    this.objectManager.createNewObjects(ids);
    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    ManagedObject mo = lookedUpObjects.get(id);
    mo.apply(new TestArrayDNA(id), new TransactionID(1), new BackReferences(), imo, false);
    this.objectManager.releaseAll(this.NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(id, -1);
    assertTrue(facade.isArray());
    assertFalse(facade.isInnerClass());
    assertFalse(facade.isMap());
    assertFalse(facade.isList());
    assertFalse(facade.isSet());
    assertEquals(3, facade.getArrayLength());
    assertTrue(Arrays.equals(new String[] { "0", "1", "2" }, facade.getFields()));
    assertEquals("[Ljava/lang/String;", facade.getClassName());

    for (int i = 0; i < 3; i++) {
      assertEquals("String", facade.getFieldType("" + i));
    }

    assertEquals("tim", facade.getFieldValue("0"));
    assertEquals("is", facade.getFieldValue("1"));
    assertEquals("here", facade.getFieldValue("2"));

    // test that limit is working okay
    facade = this.objectManager.lookupFacade(id, 1);
    assertEquals(1, facade.getFields().length);
    assertEquals(3, facade.getArrayLength()); // array length is still 3 even if limit is 1
    assertEquals("tim", facade.getFieldValue("0"));

    facade = this.objectManager.lookupFacade(id, 19212);
    assertEquals(3, facade.getArrayLength());
    assertEquals("tim", facade.getFieldValue("0"));
    assertEquals("is", facade.getFieldValue("1"));
    assertEquals("here", facade.getFieldValue("2"));
  }

  public void testDateFacades() throws NoSuchObjectException {
    initObjectManager();

    ObjectID dateID = new ObjectID(1);

    ObjectIDSet ids = new ObjectIDSet();
    ids.add(dateID);
    this.objectManager.createNewObjects(ids);
    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject dateManagedObject = lookedUpObjects.get(dateID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    dateManagedObject.apply(new TestDateDNA("java.util.Date", dateID), new TransactionID(1), new BackReferences(), imo,
                            false);

    this.objectManager.releaseAll(this.NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(dateID, 1);
    validateDateFacade(facade);

  }

  public void testLiteralFacades() throws NoSuchObjectException {
    initObjectManager();

    ObjectID literalID = new ObjectID(1);

    ObjectIDSet ids = new ObjectIDSet();
    ids.add(literalID);

    this.objectManager.createNewObjects(ids);
    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject managedObject = lookedUpObjects.get(literalID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    managedObject.apply(new TestLiteralValuesDNA(literalID), new TransactionID(1), new BackReferences(), imo, false);

    this.objectManager.releaseAll(this.NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(literalID, 1);
    validateLiteralFacade(facade);

  }

  private void validateLiteralFacade(ManagedObjectFacade literalFacade) {
    assertFalse(literalFacade.isArray());
    assertFalse(literalFacade.isMap());
    assertFalse(literalFacade.isSet());
    assertFalse(literalFacade.isList());
    assertEquals("java.lang.Integer", literalFacade.getClassName());

    Object value = literalFacade.getFieldValue("java.lang.Integer");

    assertTrue(value instanceof Integer);
  }

  public void testLogicalFacades() throws NoSuchObjectException {
    initObjectManager();

    ObjectID mapID = new ObjectID(1);
    ObjectID listID = new ObjectID(2);
    ObjectID setID = new ObjectID(3);

    ObjectIDSet ids = new ObjectIDSet();
    ids.add(mapID);
    ids.add(listID);
    ids.add(setID);

    this.objectManager.createNewObjects(ids);
    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject list = lookedUpObjects.get(listID);
    ManagedObject set = lookedUpObjects.get(setID);
    ManagedObject map = lookedUpObjects.get(mapID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    map.apply(new TestMapDNA(mapID), new TransactionID(1), new BackReferences(), imo, false);
    set.apply(new TestListSetDNA("java.util.HashSet", setID), new TransactionID(1), new BackReferences(), imo, false);
    list.apply(new TestListSetDNA("java.util.LinkedList", listID), new TransactionID(1), new BackReferences(), imo,
               false);

    this.objectManager.releaseAll(this.NULL_TRANSACTION, lookedUpObjects.values());

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
    validateSetFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(setID, 5);
    validateSetFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(setID, 1);
    validateSetFacade(facade, 1, 3);
    facade = this.objectManager.lookupFacade(setID, 0);
    validateSetFacade(facade, 0, 3);

    facade = this.objectManager.lookupFacade(listID, -1);
    validateListFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(listID, 5);
    validateListFacade(facade, 3, 3);
    facade = this.objectManager.lookupFacade(listID, 1);
    validateListFacade(facade, 1, 3);
    facade = this.objectManager.lookupFacade(listID, 0);
    validateListFacade(facade, 0, 3);

  }

  private void validateListFacade(ManagedObjectFacade listFacade, int facadeSize, int totalSize) {
    assertFalse(listFacade.isArray());
    assertFalse(listFacade.isMap());
    assertFalse(listFacade.isSet());
    assertTrue(listFacade.isList());
    assertEquals("java.util.LinkedList", listFacade.getClassName());
    assertEquals(facadeSize, listFacade.getFacadeSize());
    assertEquals(totalSize, listFacade.getTrueObjectSize());

    for (int i = 0; i < facadeSize; i++) {
      String fName = String.valueOf(i);
      Object value = listFacade.getFieldValue(fName);
      assertTrue(value instanceof String);
      assertEquals("item" + (i + 1), value);
    }
  }

  private void validateSetFacade(ManagedObjectFacade setFacade, int facadeSize, int totalSize) {
    assertFalse(setFacade.isArray());
    assertFalse(setFacade.isMap());
    assertTrue(setFacade.isSet());
    assertFalse(setFacade.isList());
    assertEquals("java.util.HashSet", setFacade.getClassName());
    assertEquals(facadeSize, setFacade.getFacadeSize());
    assertEquals(totalSize, setFacade.getTrueObjectSize());

    Set<String> expect = new HashSet<String>();
    expect.add("item1");
    expect.add("item2");
    expect.add("item3");

    Set<String> actual = new HashSet<String>();
    for (int i = 0; i < facadeSize; i++) {
      String fName = String.valueOf(i);
      Object value = setFacade.getFieldValue(fName);
      assertTrue(value instanceof String);
      actual.add((String) value);
    }

    assertTrue(expect.containsAll(actual));
  }

  private void validateMapFacade(ManagedObjectFacade mapFacade, int facadeSize, int totalSize) {
    assertFalse(mapFacade.isArray());
    assertTrue(mapFacade.isMap());
    assertFalse(mapFacade.isSet());
    assertFalse(mapFacade.isList());
    assertEquals("java.util.HashMap", mapFacade.getClassName());
    assertEquals(facadeSize, mapFacade.getFacadeSize());
    assertEquals(totalSize, mapFacade.getTrueObjectSize());

    Map<String, String> expect = new HashMap<String, String>();
    expect.put("key1", "val1");
    expect.put("key2", "val2");
    expect.put("key3", "val3");

    Map<String, String> actual = new HashMap<String, String>();

    for (int i = 0; i < facadeSize; i++) {
      String fName = String.valueOf(i);
      Object value = mapFacade.getFieldValue(fName);
      assertTrue(value instanceof MapEntryFacade);
      MapEntryFacade entry = (MapEntryFacade) value;
      actual.put((String) entry.getKey(), (String) entry.getValue());
    }

    for (String key : actual.keySet()) {
      assertEquals(expect.get(key), actual.get(key));
    }
  }

  private void validateDateFacade(ManagedObjectFacade dateFacade) {
    assertFalse(dateFacade.isArray());
    assertFalse(dateFacade.isMap());
    assertFalse(dateFacade.isSet());
    assertFalse(dateFacade.isList());
    assertEquals("java.util.Date", dateFacade.getClassName());

    Object value = dateFacade.getFieldValue("date");

    assertTrue(value instanceof Date);
  }

  private DBEnvironment newDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new DBEnvironment(paranoid, dbHome);
  }

  private Persistor newPersistor(DBEnvironment dbEnv, SerializationAdapterFactory serializationAdapterFactory)
      throws Exception {
    return new SleepycatPersistor(this.logger, dbEnv, serializationAdapterFactory);
  }

  private SerializationAdapterFactory newSleepycatSerializationAdapterFactory(DBEnvironment dbEnv) {
    return new SleepycatSerializationAdapterFactory();
  }

  private SerializationAdapterFactory newCustomSerializationAdapterFactory() {
    return new CustomSerializationAdapterFactory();
  }

  public void testLookupInPersistentContext() throws Exception {
    boolean paranoid = false;
    // sleepycat serializer, not paranoid
    DBEnvironment dbEnv = newDBEnvironment(paranoid);
    SerializationAdapterFactory saf = newSleepycatSerializationAdapterFactory(dbEnv);
    Persistor persistor = newPersistor(dbEnv, saf);

    testLookupInPersistentContext(persistor, paranoid, new NullPersistenceTransactionProvider());

    // custom serializer, not paranoid
    dbEnv = newDBEnvironment(paranoid);
    saf = newCustomSerializationAdapterFactory();
    persistor = newPersistor(dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid, new NullPersistenceTransactionProvider());

    // sleepycat serializer, paranoid
    paranoid = true;
    dbEnv = newDBEnvironment(paranoid);
    saf = newSleepycatSerializationAdapterFactory(dbEnv);
    persistor = newPersistor(dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid, persistor.getPersistenceTransactionProvider());

    // custom serializer, paranoid
    dbEnv = newDBEnvironment(paranoid);
    saf = newCustomSerializationAdapterFactory();
    persistor = newPersistor(dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid, persistor.getPersistenceTransactionProvider());
  }

  private void testLookupInPersistentContext(Persistor persistor, boolean paranoid, PersistenceTransactionProvider ptp)
      throws Exception {
    ManagedObjectPersistor mop = persistor.getManagedObjectPersistor();
    PersistentManagedObjectStore store = new PersistentManagedObjectStore(mop, new MockSink());
    TestSink faultSink = new TestSink();
    TestSink flushSink = new TestSink();
    this.config.paranoid = paranoid;
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store, new LRUEvictionPolicy(100),
                                               this.persistenceTransactionProvider, faultSink, flushSink,
                                               this.objectStatsRecorder);
    new TestMOFaulter(this.objectManager, store, faultSink, new NullSinkContext()).start();
    new TestMOFlusher(this.objectManager, flushSink, new NullSinkContext()).start();

    ObjectID id = new ObjectID(1);
    ObjectIDSet ids = new ObjectIDSet();
    ids.add(id);
    ClientID key = new ClientID(0);

    this.objectManager.createNewObjects(ids);
    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(key, responseContext);

    ManagedObject lookedUpViaLookupObjectsForCreateIfNecessary = lookedUpObjects.get(id);

    final String fieldName = "myField";
    final List<Integer> countSlot = new ArrayList<Integer>(1);
    countSlot.add(1);
    final List<ObjectID> fieldValueSlot = new ArrayList<ObjectID>(1);
    fieldValueSlot.add(new ObjectID(100));

    DNACursor cursor = new DNACursor() {
      public LogicalAction getLogicalAction() {
        return null;
      }

      public PhysicalAction getPhysicalAction() {
        return new PhysicalAction(fieldName, fieldValueSlot.get(0), true);
      }

      public boolean next() {
        int count = countSlot.get(0).intValue();
        count--;
        countSlot.set(0, count);
        return count >= 0;
      }

      public boolean next(DNAEncoding encoding) {
        throw new ImplementMe();
      }

      public Object getAction() {
        throw new ImplementMe();
      }

      public int getActionCount() {
        return 1;
      }

      public void reset() throws UnsupportedOperationException {
        countSlot.set(0, 1);
      }
    };

    TestDNA dna = new TestDNA(cursor);
    dna.version = 5;

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(1), new BackReferences(), imo, false);

    PersistenceTransaction tx = ptp.newTransaction();
    this.objectManager.release(tx, lookedUpViaLookupObjectsForCreateIfNecessary);

    ManagedObject lookedUpViaLookup = this.objectManager.getObjectByID(id);
    assertEquals(1, lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().size());
    assertEquals(lookedUpViaLookup.getObjectReferences(), lookedUpViaLookupObjectsForCreateIfNecessary
        .getObjectReferences());

    tx = ptp.newTransaction();
    this.objectManager.release(tx, lookedUpViaLookup);

    // now do another lookup, change, and commit cycle
    responseContext = new TestResultsContext(ids, new ObjectIDSet());
    lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(key, responseContext);
    lookedUpViaLookupObjectsForCreateIfNecessary = lookedUpObjects.get(id);
    countSlot.set(0, 1);
    ObjectID newReferenceID = new ObjectID(9324);
    fieldValueSlot.set(0, newReferenceID);
    dna = new TestDNA(cursor);
    dna.version = 10;
    dna.isDelta = true;
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(2), new BackReferences(), imo, false);
    // lookedUpViaLookupObjectsForCreateIfNecessary.commit();
    tx = ptp.newTransaction();
    this.objectManager.release(tx, lookedUpViaLookupObjectsForCreateIfNecessary);

    lookedUpViaLookup = this.objectManager.getObjectByID(id);
    assertEquals(1, lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().size());
    assertTrue(lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().contains(newReferenceID));

    assertEquals(lookedUpViaLookup.getObjectReferences(), lookedUpViaLookupObjectsForCreateIfNecessary
        .getObjectReferences());

    close(persistor, store);
  }

  private static void close(Persistor persistor, PersistentManagedObjectStore store) {
    // to work around timing problem with this test, let's look up some object id...
    // this should block this thread until trasaction reading all object ids from bdb completes,
    // at which point, it's ok to close the DB
    persistor.getManagedObjectPersistor().getAllObjectIDs().size();
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
    ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>());
    this.objectManager.createObject(mo);
    assertFalse(this.objectManager.isReferenced(id));
    ManagedObject mo2 = this.objectManager.getObjectByID(id);
    assertTrue(mo == mo2);
    assertTrue(this.objectManager.isReferenced(id));
    this.objectManager.release(this.NULL_TRANSACTION, mo);
    assertFalse(this.objectManager.isReferenced(id));

    this.objectManager.getObjectByID(id);

    final boolean[] gotIt = new boolean[1];
    gotIt[0] = false;

    Thread t = new Thread() {
      @Override
      public void run() {
        ObjectManagerTest.this.objectManager.getObjectByID(id);
        gotIt[0] = true;
      }
    };

    t.start();
    ThreadUtil.reallySleep(1000);
    assertFalse(gotIt[0]);
    this.objectManager.release(this.NULL_TRANSACTION, mo);
    ThreadUtil.reallySleep(1000);
    assertTrue(gotIt[0]);
  }

  public void testPhysicalObjectFacade() throws Exception {
    testPhysicalObjectFacade(false);
    testPhysicalObjectFacade(true);
  }

  private void testPhysicalObjectFacade(boolean paranoid) throws Exception {
    DBEnvironment dbEnv = newDBEnvironment(paranoid);
    SerializationAdapterFactory saf = newCustomSerializationAdapterFactory();
    Persistor persistor = newPersistor(dbEnv, saf);
    PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    PersistentManagedObjectStore persistantMOStore = new PersistentManagedObjectStore(persistor
        .getManagedObjectPersistor(), new MockSink());
    this.objectStore = persistantMOStore;
    this.config.paranoid = paranoid;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), new NullCache(),
                      this.objectStore);

    ObjectIDSet oids = new ObjectIDSet();
    oids.add(new ObjectID(1));

    this.objectManager.createNewObjects(oids);
    final TestResultsContext context = new TestResultsContext(oids, oids);
    this.objectManager.lookupObjectsFor(null, context);
    context.waitTillComplete();
    ManagedObject mo = (context.objects).get(new ObjectID(1));
    assertTrue(mo.isNew());
    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    mo.apply(new TestPhysicalDNA(new ObjectID(1)), new TransactionID(1), new BackReferences(), imo, false);

    PersistenceTransaction tx = ptp.newTransaction();
    this.objectManager.release(tx, mo);

    ManagedObjectFacade facade;
    try {
      facade = this.objectManager.lookupFacade(new ObjectID(1), -1);
    } catch (NoSuchObjectException e1) {
      fail(e1.getMessage());
      return;
    }

    String[] fieldNames = facade.getFields();
    assertEquals(6, fieldNames.length);
    // NOTE: the order of the object fields should be alphabetic
    assertTrue(Arrays.asList(fieldNames).toString(), Arrays.equals(fieldNames, new String[] { "access$0", "this$0",
        "intField", "objField", "stringField", "zzzField" }));
    assertEquals("TestPhysicalDNA.class.name", facade.getClassName());
    assertEquals("Integer", facade.getFieldType("intField"));
    assertEquals("ObjectID", facade.getFieldType("objField"));
    assertEquals("Byte", facade.getFieldType("zzzField"));
    assertEquals("String", facade.getFieldType("stringField"));
    assertEquals(42, facade.getFieldValue("intField"));
    assertEquals((byte) 1, facade.getFieldValue("zzzField"));
    assertEquals(new ObjectID(696969), facade.getFieldValue("objField"));
    assertEquals("yo yo yo", facade.getFieldValue("stringField"));
    assertEquals(new ObjectID(1), facade.getObjectId());
    assertTrue(facade.isPrimitive("intField"));
    assertTrue(facade.isPrimitive("zzzField"));
    assertTrue(facade.isPrimitive("stringField"));
    assertFalse(facade.isPrimitive("objField"));

    try {
      facade.getFieldType("does not exist");
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      facade.getFieldValue("does not exist");
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      facade.isPrimitive("does not exist");
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    close(persistor, persistantMOStore);
    // XXX: change the object again, make sure the facade is "stable" (ie.
    // doesn't change)
  }

  public void testObjectManagerAsync() {
    initObjectManager();
    final ObjectID id = new ObjectID(0);
    final ObjectID id1 = new ObjectID(1);

    ObjectIDSet objectIDs = new ObjectIDSet();

    ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>());
    ManagedObject mo1 = new TestManagedObject(id1, new ArrayList<ObjectID>());
    this.objectManager.createObject(mo);
    this.objectManager.createObject(mo1);

    assertFalse(this.objectManager.isReferenced(id));

    objectIDs.add(id);

    TestObjectManagerResultsContext context;
    assertTrue(this.objectManager
        .lookupObjectsAndSubObjectsFor(
                                       null,
                                       context = new TestObjectManagerResultsContext(
                                                                                     new HashMap<ObjectID, ManagedObject>(),
                                                                                     objectIDs), -1));

    ManagedObject retrievedMo = (ManagedObject) context.getResults().values().iterator().next();
    assertTrue(mo == retrievedMo);
    assertTrue(this.objectManager.isReferenced(id));
    this.objectManager.release(this.NULL_TRANSACTION, mo);
    assertFalse(this.objectManager.isReferenced(id));

    this.objectManager.getObjectByID(id);

    objectIDs.add(id1);

    boolean notPending = this.objectManager
        .lookupObjectsAndSubObjectsFor(
                                       null,
                                       context = new TestObjectManagerResultsContext(
                                                                                     new HashMap<ObjectID, ManagedObject>(),
                                                                                     objectIDs), -1);
    assertFalse(notPending);
    assertEquals(0, context.getResults().size());
    this.objectManager.release(this.NULL_TRANSACTION, mo);
    assertEquals(objectIDs.size(), context.getResults().size());

    Collection objs = context.getResults().values();
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

  public void testCacheStats() throws Exception {
    this.config.paranoid = true;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))),
                      new LRUEvictionPolicy(-1));
    this.objectManager.setStatsListener(this.stats);

    assertEquals(0, this.stats.getTotalRequests());
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(0, this.stats.getTotalCacheMisses());

    createObjects(50, 10);
    ObjectIDSet ids = makeObjectIDSet(0, 10);
    // ThreadUtil.reallySleep(5000);
    TestResultsContext results = new TestResultsContext(ids, new ObjectIDSet());

    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());

    assertEquals(10, this.stats.getTotalRequests());
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(10, this.stats.getTotalCacheMisses());

    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(20, this.stats.getTotalRequests());
    assertEquals(10, this.stats.getTotalCacheHits());
    assertEquals(10, this.stats.getTotalCacheMisses());

    ids = makeObjectIDSet(10, 20);
    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(30, this.stats.getTotalRequests());
    assertEquals(10, this.stats.getTotalCacheHits());
    assertEquals(20, this.stats.getTotalCacheMisses());

    evictCache(10);

    ids = makeObjectIDSet(14, 4);
    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAll(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(40, this.stats.getTotalRequests());
    assertEquals(15, this.stats.getTotalCacheHits());
    assertEquals(25, this.stats.getTotalCacheMisses());

    double hitRate = ((double) 15) / ((double) 40);
    assertEquals(hitRate, this.stats.getCacheHitRatio(), 0D);
  }

  private void evictCache(int inCache) {
    TestCacheStats tc = new TestCacheStats();
    tc.toKeep = inCache;
    this.objectManager.evictCache(tc);
    tc.validate();
  }

  private void createObjects(int num, int inCache) {
    createObjects(num);
    evictCache(inCache);
  }

  private ObjectIDSet makeObjectIDSet(int begin, int end) {
    ObjectIDSet rv = new ObjectIDSet();

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

  private void createObjects(int num) {
    for (int i = 0; i < num; i++) {
      TestManagedObject mo = new TestManagedObject(new ObjectID(i), new ArrayList<ObjectID>());
      this.objectManager.createObject(mo);
      this.objectStore.addNewObject(mo);
    }
  }

  /**
   * public void testGCStats() { initObjectManager(); // this should disable the internal gc thread, allowing us to
   * control when // objMgr.gc() happens this.config.myGCThreadSleepTime = -1; GarbageCollector gc = new
   * MarkAndSweepGarbageCollector(objectManager, clientStateManager, new ObjectManagerConfig(300000, true, true, false,
   * false, 60000)); objectManager.setGarbageCollector(gc); objectManager.start(); objectManager.createRoot("root-me",
   * new ObjectID(0)); ManagedObject root = new TestManagedObject(new ObjectID(0), new ObjectID[] { new ObjectID(1) });
   * objectManager.createObject(root); this.objectStore.addNewObject(root); TestManagedObject mo1 = new
   * TestManagedObject(new ObjectID(1), new ObjectID[] { new ObjectID(2) }); TestManagedObject mo2 = new
   * TestManagedObject(new ObjectID(2), new ObjectID[] { new ObjectID(3) }); TestManagedObject mo3 = new
   * TestManagedObject(new ObjectID(3), new ObjectID[] {}); objectManager.createObject(mo1);
   * this.objectStore.addNewObject(mo1); objectManager.createObject(mo2); this.objectStore.addNewObject(mo2);
   * objectManager.createObject(mo3); this.objectStore.addNewObject(mo3); ClientID cid1 = new ClientID(new
   * ChannelID(1)); clientStateManager.addReference(cid1, root.getID()); clientStateManager.addReference(cid1,
   * mo1.getID()); clientStateManager.addReference(cid1, mo2.getID()); clientStateManager.addReference(cid1,
   * mo3.getID()); // assertEquals(0, objectManager.getGarbageCollectorStats().length); // assertEquals(0,
   * listener.gcEvents.size()); long start = System.currentTimeMillis(); objectManager.getGarbageCollector().gc(); //
   * assertEquals(1, objectManager.getGarbageCollectorStats().length); // assertEquals(3, listener.gcEvents.size());
   * GCStats stats1 = listener.gcEvents.get(0); final int firstIterationNumber = stats1.getIteration();
   * assertSame(stats1, objectManager.getGarbageCollectorStats()[0]); assertTrue("external: " + start + ", reported: " +
   * stats1.getStartTime(), stats1.getStartTime() >= start); assertTrue(String.valueOf(stats1.getElapsedTime()),
   * stats1.getElapsedTime() >= 0); assertEquals(4, stats1.getBeginObjectCount()); assertEquals(0,
   * stats1.getCandidateGarbageCount()); assertEquals(0, stats1.getActualGarbageCount()); listener.gcEvents.clear();
   * objectManager.getGarbageCollector().gc(); assertEquals(2, objectManager.getGarbageCollectorStats().length);
   * assertEquals(3, listener.gcEvents.size()); assertEquals(firstIterationNumber + 1,
   * objectManager.getGarbageCollectorStats()[0].getIteration()); listener.gcEvents.clear(); Set<ObjectID> removed = new
   * HashSet<ObjectID>(); removed.add(mo3.getID()); clientStateManager.removeReferences(cid1, removed);
   * mo2.setReferences(new ObjectID[] {}); objectManager.getGarbageCollector().gc(); assertEquals(3,
   * objectManager.getGarbageCollectorStats().length); assertEquals(3, listener.gcEvents.size()); GCStats stats3 =
   * listener.gcEvents.get(0); assertEquals(4, stats3.getBeginObjectCount()); assertEquals(1,
   * stats3.getActualGarbageCount()); assertEquals(1, stats3.getCandidateGarbageCount()); }
   */

  public void testLookupFacadeForMissingObject() {
    initObjectManager();

    try {
      this.objectManager.lookupFacade(new ObjectID(1), -1);
      fail("lookup didn't throw exception");
    } catch (NoSuchObjectException e) {
      // expected
    }
  }

  public void testObjectManagerGC() throws Exception {
    initObjectManager();
    // this should disable the gc thread.
    this.config.myGCThreadSleepTime = -1;
    TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();
    final ObjectID id = new ObjectID(0);
    ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>(3));
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

    Thread gcCaller = new Thread(new GCCaller(), "GCCaller");
    gcCaller.start();

    // give the thread some time to start and call collect()...
    assertTrue(gc.waitForCollectToBeCalled(5000));

    // give the thread some time to call blockUntilReadyToGC()...
    assertTrue(gc.waitFor_blockUntilReadyToGC_ToBeCalled(5000));

    // ////////////////////////////////////////////////////
    // now call release and make sure it calls the appropriate GC methods...

    assertFalse(gc.notifyReadyToGC_WasCalled());
    this.objectManager.release(this.NULL_TRANSACTION, mo);

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
    DBEnvironment dbEnv = newDBEnvironment(true);
    SerializationAdapterFactory saf = newCustomSerializationAdapterFactory();
    Persistor persistor = newPersistor(dbEnv, saf);
    PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    PersistentManagedObjectStore persistentMOStore = new PersistentManagedObjectStore(persistor
        .getManagedObjectPersistor(), new MockSink());
    this.objectStore = persistentMOStore;
    this.config.paranoid = true;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), new NullCache(),
                      this.objectStore);
    initTransactionObjectManager();

    // this should disable the gc thread.
    this.config.myGCThreadSleepTime = -1;
    TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    /**
     * STEP 1: Create an New object and check it out
     */
    Map<ObjectID, DNA> changes = new HashMap<ObjectID, DNA>();

    changes.put(new ObjectID(1), new TestPhysicalDNA(new ObjectID(1)));

    ServerTransaction stxn1 = new ServerTransactionImpl(new TxnBatchID(1), new TransactionID(1), new SequenceID(1),
                                                        new LockID[0], new ClientID(2), new ArrayList<DNA>(changes
                                                            .values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY, 1, new long[0]);
    List<ServerTransaction> txns = new ArrayList<ServerTransaction>();
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
    applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(stxn1.getServerTransactionID());
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    CommitTransactionContext ctc1 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc1);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc1);
    Collection applied = ctc1.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn1.getServerTransactionID(), applied.iterator().next());
    Collection objects = ctc1.getObjects();
    assertTrue(objects.size() == 1);

    /**
     * STEP 2: Dont check back Object 1 yet, make another transaction with yet another object
     */
    changes.clear();
    changes.put(new ObjectID(2), new TestPhysicalDNA(new ObjectID(2)));

    ServerTransaction stxn2 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2), new SequenceID(1),
                                                        new LockID[0], new ClientID(2), new ArrayList<DNA>(changes
                                                            .values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY, 1, new long[0]);

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
    changes.put(new ObjectID(1), new TestPhysicalDNA(new ObjectID(1), true));
    changes.put(new ObjectID(2), new TestPhysicalDNA(new ObjectID(2), true));
    changes.put(new ObjectID(3), new TestPhysicalDNA(new ObjectID(3)));

    ServerTransaction stxn3 = new ServerTransactionImpl(new TxnBatchID(2), new TransactionID(2), new SequenceID(1),
                                                        new LockID[0], new ClientID(2), new ArrayList<DNA>(changes
                                                            .values()), new ObjectStringSerializer(),
                                                        Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                        DmiDescriptor.EMPTY_ARRAY, 1, new long[0]);

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
    applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(stxn2.getServerTransactionID());
    acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    CommitTransactionContext ctc2 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc2);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc2);
    applied = ctc2.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn2.getServerTransactionID(), applied.iterator().next());
    objects = ctc2.getObjects();
    assertTrue(objects.size() == 1);

    /**
     * STEP 4: Check in Object 1 thus releasing the blocked lookup for Object 1, 3
     */

    // Now check back Object 1
    PersistenceTransaction dbtxn = ptp.newTransaction();
    this.objectManager.releaseAll(dbtxn, ctc1.getObjects());

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
    Thread t = new Thread("GC Thread - testRecallNewObjects") {
      @Override
      public void run() {
        ObjectManagerTest.this.objectManager.waitUntilReadyToGC();
        try {
          cb.await();
        } catch (Exception e) {
          e.printStackTrace();
          throw new AssertionError(e);
        }
      }
    };
    t.start();
    ThreadUtil.reallySleep(5000);

    // Recall request should have be added.
    RecallObjectsContext roc = (RecallObjectsContext) this.coordinator.recallSink.queue.take();
    assertNotNull(roc);
    assertTrue(this.coordinator.recallSink.queue.isEmpty());

    assertTrue(roc.recallAll());

    // do recall - This used to cause an assertion error in persistent mode
    this.txObjectManager.recallCheckedoutObject(roc);

    // Check in Object 2 to make the GC go to paused state
    dbtxn = ptp.newTransaction();
    this.objectManager.releaseAll(dbtxn, ctc2.getObjects());

    cb.await();

    assertTrue(gc.isPaused());

    // Complete gc
    gc.deleteGarbage(new GCResultContext(TCCollections.EMPTY_OBJECT_ID_SET, new GarbageCollectionInfo()));

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
    applyTxn(aoc);
    this.txObjectManager.applyTransactionComplete(stxn3.getServerTransactionID());
    acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    CommitTransactionContext ctc3 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
    assertNotNull(ctc3);
    assertTrue(this.coordinator.commitSink.queue.isEmpty());

    this.txObjectManager.commitTransactionsComplete(ctc3);
    applied = ctc3.getAppliedServerTransactionIDs();
    assertTrue(applied.size() == 1);
    assertEquals(stxn3.getServerTransactionID(), applied.iterator().next());
    objects = ctc3.getObjects();
    assertTrue(objects.size() == 3);

    // Now check back the objects
    dbtxn = ptp.newTransaction();
    this.objectManager.releaseAll(dbtxn, ctc3.getObjects());

    assertEquals(0, this.objectManager.getCheckedOutCount());
    assertFalse(this.objectManager.isReferenced(new ObjectID(1)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(2)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(3)));

    close(persistor, persistentMOStore);
  }

  private void applyTxn(ApplyTransactionContext aoc) {
    ServerTransaction txn = aoc.getTxn();
    Map managedObjects = aoc.getObjects();
    ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    for (Iterator i = txn.getChanges().iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      ManagedObject mo = (ManagedObject) managedObjects.get(dna.getObjectID());
      mo.apply(new VersionizedDNAWrapper(dna, ++this.version), txn.getTransactionID(), new BackReferences(),
               instanceMonitor, false);
    }
  }

  private static class TestArrayDNA implements DNA {

    private static int     _version;

    private final ObjectID id;
    private final int      version;

    private final boolean  delta;

    public TestArrayDNA(ObjectID id) {
      this(id, false);
    }

    public TestArrayDNA(ObjectID id, boolean delta) {
      this.id = id;
      this.delta = delta;
      this.version = getNextVersion();
    }

    private static int getNextVersion() {
      return _version++;
    }

    public long getVersion() {
      return this.version;
    }

    public boolean hasLength() {
      return true;
    }

    public int getArraySize() {
      return 3;
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public String getTypeName() {
      return "[Ljava/lang/String;";
    }

    public ObjectID getObjectID() throws DNAException {
      return this.id;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    public DNACursor getCursor() {
      return new DNACursor() {
        int count = 0;

        public boolean next() {
          this.count++;
          return this.count <= 2;
        }

        public LogicalAction getLogicalAction() {
          throw new ImplementMe();
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public PhysicalAction getPhysicalAction() {
          switch (this.count) {
            case 1:
              return new PhysicalAction(new String[] { "tim", "was", "here" });
            case 2:
              return new PhysicalAction(1, "is", false);
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 2;
        }

        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    public boolean isDelta() {
      return this.delta;
    }
  }

  private static class TestListSetDNA implements DNA {

    final ObjectID setID;
    final String   className;

    public TestListSetDNA(String className, ObjectID setID) {
      this.className = className;
      this.setID = setID;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public int getArraySize() {
      return -1;
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public String getTypeName() {
      return this.className;
    }

    public ObjectID getObjectID() throws DNAException {
      return this.setID;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    public DNACursor getCursor() {
      return new DNACursor() {
        int count;

        public boolean next() {
          this.count++;
          return this.count <= 3;
        }

        public LogicalAction getLogicalAction() {
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              Object item = new UTF8ByteDataHolder("item" + this.count);
              return new LogicalAction(SerializationUtil.ADD, new Object[] { item });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 3;
        }

        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    public boolean isDelta() {
      return false;
    }

  }

  private static class TestMapDNA implements DNA {

    final ObjectID        objectID;
    private final boolean isDelta;

    TestMapDNA(ObjectID id) {
      this(id, false);
    }

    TestMapDNA(ObjectID id, boolean isDelta) {
      this.objectID = id;
      this.isDelta = isDelta;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public int getArraySize() {
      return -1;
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public String getTypeName() {
      return "java.util.HashMap";
    }

    public ObjectID getObjectID() throws DNAException {
      return this.objectID;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    public DNACursor getCursor() {
      return new DNACursor() {

        int count = 0;

        public boolean next() {
          this.count++;
          return this.count <= 3;
        }

        public LogicalAction getLogicalAction() {
          switch (this.count) {
            case 1:
            case 2:
            case 3:
              Object key = new UTF8ByteDataHolder("key" + this.count);
              Object val = new UTF8ByteDataHolder("val" + this.count);
              return new LogicalAction(SerializationUtil.PUT, new Object[] { key, val });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 3;
        }

        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    public boolean isDelta() {
      return this.isDelta;
    }

  }

  private static class TestDateDNA implements DNA {

    final ObjectID setID;
    final String   className;

    public TestDateDNA(String className, ObjectID setID) {
      this.className = className;
      this.setID = setID;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public int getArraySize() {
      return -1;
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public String getTypeName() {
      return this.className;
    }

    public ObjectID getObjectID() throws DNAException {
      return this.setID;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    public DNACursor getCursor() {
      return new DNACursor() {
        int count;

        public boolean next() {
          this.count++;
          return this.count <= 1;
        }

        public LogicalAction getLogicalAction() {
          switch (this.count) {
            case 1:
              return new LogicalAction(SerializationUtil.SET_TIME, new Object[] { System.currentTimeMillis() });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 1;
        }

        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

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
    private final boolean               updateStats;

    public TestResultsContext(ObjectIDSet ids, ObjectIDSet newIDS, boolean updateStats) {
      this.ids = ids;
      this.newIDS = newIDS;
      this.updateStats = updateStats;
    }

    public TestResultsContext(ObjectIDSet ids, ObjectIDSet newIDS) {
      this(ids, newIDS, true);
    }

    public synchronized void waitTillComplete() {
      while (!this.complete) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }

    public synchronized void setResults(ObjectManagerLookupResults results) {
      this.complete = true;
      this.objects.putAll(results.getObjects());
      this.missing.addAll(results.getMissingObjectIDs());
      // if (!results.getMissingObjectIDs().isEmpty()) { throw new AssertionError("Missing Object : "
      // + results.getMissingObjectIDs()); }
      notifyAll();
    }

    public ObjectIDSet getLookupIDs() {
      return this.ids;
    }

    public ObjectIDSet getNewObjectIDs() {
      return this.newIDS;
    }

    public boolean updateStats() {
      return this.updateStats;
    }
  }

  private static class TestPhysicalDNA implements DNA {
    private final ObjectID id;
    private final boolean  isDelta;

    TestPhysicalDNA(ObjectID id) {
      this(id, false);
    }

    public TestPhysicalDNA(ObjectID id, boolean isDelta) {
      this.isDelta = isDelta;
      this.id = id;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public String getDefiningLoaderDescription() {
      return "System";
    }

    public int getArraySize() {
      return -1;
    }

    public String getTypeName() {
      return "TestPhysicalDNA.class.name";
    }

    public ObjectID getObjectID() throws DNAException {
      return this.id;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return new ObjectID(25);
    }

    public void setHeaderInformation(ObjectID id, ObjectID parentID, String typeName, int length, long version) {
      //
    }

    public void addLogicalAction(int method, Object[] parameters) {
      //
    }

    public void addPhysicalAction(String field, Object value) throws DNAException {
      //
    }

    public DNACursor getCursor() {
      return new DNACursor() {

        int count = 0;

        public boolean next() {
          this.count++;
          return this.count < 7;
        }

        public LogicalAction getLogicalAction() {
          return null;
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public PhysicalAction getPhysicalAction() {
          switch (this.count) {
            case 1: {
              return new PhysicalAction("intField", 42, false);
            }
            case 2: {
              return new PhysicalAction("zzzField", (byte) 1, false);
            }
            case 3: {
              return new PhysicalAction("objField", new ObjectID(696969), true);
            }
            case 4: {
              return new PhysicalAction("this$0", new ObjectID(25), true);
            }
            case 5: {
              return new PhysicalAction("access$0", new Float(2.4), false);
            }
            case 6: {
              return new PhysicalAction("stringField", new UTF8ByteDataHolder("yo yo yo"), false);
            }
            default: {
              throw new RuntimeException();
            }
          }

        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 6;
        }

        public void reset() throws UnsupportedOperationException {
          this.count = 0;
        }

      };
    }

    public boolean isDelta() {
      return this.isDelta;
    }
  }

  private static class TestLiteralValuesDNA implements DNA {
    private final ObjectID id;

    TestLiteralValuesDNA(ObjectID id) {
      this.id = id;
    }

    public long getVersion() {
      return 0;
    }

    public boolean hasLength() {
      return false;
    }

    public String getDefiningLoaderDescription() {
      return "";
    }

    public int getArraySize() {
      return -1;
    }

    public String getTypeName() {
      return "java.lang.Integer";
    }

    public ObjectID getObjectID() throws DNAException {
      return this.id;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return new ObjectID(25);
    }

    public void setHeaderInformation(ObjectID id, ObjectID parentID, String typeName, int length, long version) {
      //
    }

    public void addLogicalAction(int method, Object[] parameters) {
      //
    }

    public void addPhysicalAction(String field, Object value) throws DNAException {
      //
    }

    public DNACursor getCursor() {
      return new DNACursor() {

        int count = 0;

        public boolean next() {
          this.count++;
          return this.count < 2;
        }

        public LogicalAction getLogicalAction() {
          return null;
        }

        public Object getAction() {
          switch (this.count) {
            case 1: {
              return new LiteralAction(42);
            }
            default: {
              throw new RuntimeException();
            }
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(DNAEncoding encoding) {
          throw new ImplementMe();
        }

        public int getActionCount() {
          return 1;
        }

        public void reset() throws UnsupportedOperationException {
          throw new ImplementMe();
        }
      };
    }

    public boolean isDelta() {
      return false;
    }

  }

  private class GCCaller implements Runnable {

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
      super(10000, true, true, true, false, 60000);
    }

    TestObjectManagerConfig(long gcThreadSleepTime, boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, false, 60000);
      throw new RuntimeException("Don't use me.");
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

  private static class TestMOFaulter extends Thread {

    private final ObjectManagerImpl  objectManager;
    private final ManagedObjectStore store;
    private final TestSink           faultSink;
    private final SinkContext        sinkContext;

    public TestMOFaulter(ObjectManagerImpl objectManager, ManagedObjectStore store, TestSink faultSink,
                         SinkContext sinkContext) {
      this.store = store;
      this.faultSink = faultSink;
      this.objectManager = objectManager;
      this.sinkContext = sinkContext;
      setName("TestMOFaulter");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        try {
          ManagedObjectFaultingContext ec = (ManagedObjectFaultingContext) this.faultSink.take();
          this.objectManager.addFaultedObject(ec.getId(), this.store.getObjectByID(ec.getId()), ec.isRemoveOnRelease());
          this.sinkContext.postProcess();

        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class TestMOFlusher extends Thread {

    private final ObjectManagerImpl objectManager;
    private final TestSink          flushSink;
    private final SinkContext       sinkContext;

    public TestMOFlusher(ObjectManagerImpl objectManager, TestSink flushSink, SinkContext sinkContext) {
      this.objectManager = objectManager;
      this.flushSink = flushSink;
      this.sinkContext = sinkContext;
      setName("TestMOFlusher");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (true) {
        try {
          ManagedObjectFlushingContext ec = (ManagedObjectFlushingContext) this.flushSink.take();
          this.objectManager.flushAndEvict(ec.getObjectToFlush());
          this.sinkContext.postProcess();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class TestSinkContext implements SinkContext {
    private final Counter counter = new Counter(0);

    public int getCounter() {
      return this.counter.get();
    }

    public void waitUntillCounterIs(int count) {
      this.counter.waitUntil(count);
    }

    public void resetCounter() {
      this.counter.reset();
    }

    public synchronized void postProcess() {
      this.counter.increment();
    }
  }

  private static class NullSinkContext implements SinkContext {

    public void postProcess() {
      // do nothing
    }

  }

  private interface SinkContext {

    void postProcess();

  }

}
