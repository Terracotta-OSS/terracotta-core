/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import EDU.oswego.cs.dl.util.concurrent.Latch;

import com.tc.async.impl.MockSink;
import com.tc.exception.ImplementMe;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.cache.CacheStats;
import com.tc.object.cache.Cacheable;
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
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.ApplyCompleteEventContext;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.PeriodicDGCResultContext;
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
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.mgmt.MapEntryFacade;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.db.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.persistence.db.SerializationAdapterFactory;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionImpl;
import com.tc.objectserver.tx.ServerTransactionSequencerImpl;
import com.tc.objectserver.tx.TestTransactionalStageCoordinator;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.test.TCTestCase;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.Counter;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.TCCollections;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
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
public class ObjectManagerTest extends TCTestCase {

  private Map                                managed;
  private ObjectManagerImpl                  objectManager;
  private TestObjectManagerConfig            config;
  private ClientStateManager                 clientStateManager;
  private ManagedObjectStore                 objectStore;
  private TCLogger                           logger;
  private ObjectManagerStatsImpl             stats;
  private SampledCounter                     newObjectCounter;
  private SampledCounterImpl                 objectfaultCounter;
  private SampledCounterImpl                 objectflushCounter;
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
  public ObjectManagerTest(final String arg0) {
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
    this.objectflushCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.stats = new ObjectManagerStatsImpl(this.newObjectCounter, this.objectfaultCounter, this.objectflushCounter);
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

  private void initObjectManager(final ThreadGroup threadGroup) {
    initObjectManager(threadGroup, new NullCache());
  }

  private void initObjectManager(final ThreadGroup threadGroup, final EvictionPolicy cache) {
    this.objectStore = new InMemoryManagedObjectStore(this.managed);
    initObjectManager(threadGroup, cache, this.objectStore);
  }

  private void initObjectManager(final ThreadGroup threadGroup, final EvictionPolicy cache,
                                 final ManagedObjectStore store) {
    final TestSink faultSink = new TestSink();
    final TestSink flushSink = new TestSink();
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store, cache,
                                               this.persistenceTransactionProvider, faultSink, flushSink,
                                               this.objectStatsRecorder);
    this.testFaultSinkContext = new TestSinkContext();
    new TestMOFaulter(this.objectManager, store, faultSink, this.testFaultSinkContext).start();
    new TestMOFlusher(this.objectManager, flushSink, new NullSinkContext()).start();
  }

  private TestMOFlusherWithLatch initObjectManagerAndGetFlusher(final ThreadGroup threadGroup,
                                                                final EvictionPolicy cache) {
    final TestSink faultSink = new TestSink();
    final TestSink flushSink = new TestSink();
    this.objectStore = new InMemoryManagedObjectStore(this.managed);
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, this.objectStore, cache,
                                               this.persistenceTransactionProvider, faultSink, flushSink,
                                               this.objectStatsRecorder);
    this.testFaultSinkContext = new TestSinkContext();
    new TestMOFaulter(this.objectManager, this.objectStore, faultSink, this.testFaultSinkContext).start();
    TestMOFlusherWithLatch flusherWithLatch = new TestMOFlusherWithLatch(objectManager, flushSink,
                                                                         this.testFaultSinkContext);
    flusherWithLatch.start();
    return flusherWithLatch;
  }

  private void initTransactionObjectManager() {
    final ServerTransactionSequencerImpl sequencer = new ServerTransactionSequencerImpl();
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
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))),
                      new LRUEvictionPolicy(-1));
    this.objectManager.setStatsListener(this.stats);

    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(0, this.stats.getTotalCacheMisses());

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
    this.testFaultSinkContext.resetCounter();

    final ClientID c1 = new ClientID(1);
    // fetch 10 objects and with fault-count -1
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, -1);
    Assert.assertEquals(10, results.objects.size());
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

    // fetch 10 objects and with fault-count 1K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 1000);
    Assert.assertEquals(1000, results.objects.size());
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

    // fetch 10 objects and with fault-count 10K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 10000);
    Assert.assertEquals(10000, results.objects.size());
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

    // fetch 10 objects and with fault-count 20K. but, max objects available are 10010
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 20000);
    Assert.assertEquals(10010, results.objects.size());
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

    // single object reaching more objects than fault count
    createObjects(10, 11, createObjects(11000, 18000, new HashSet<ObjectID>()));

    ids = makeObjectIDSet(10, 11);
    results = new TestResultsContext(ids, new ObjectIDSet(), true);
    this.testFaultSinkContext.resetCounter();

    // fetch 1 object and with fault-count 5K. but, object can reach 7K
    this.objectManager.lookupObjectsAndSubObjectsFor(c1, results, 5000);
    Assert.assertEquals(5000, results.objects.size());
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

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
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

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
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

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
    final ObjectIDSet ids = makeObjectIDSet(1, 2);
    final TestResultsContext result1 = new TestResultsContext(ids, new ObjectIDSet(), true);

    this.testFaultSinkContext.resetCounter();
    this.objectManager.lookupObjectsAndSubObjectsFor(null, result1, -1);
    result1.waitTillComplete();
    assertEquals(0, this.testFaultSinkContext.getCounter());

    // Now look two missing objects
    final ObjectIDSet missingids = makeObjectIDSet(20, 22);
    final TestResultsContext result2 = new TestResultsContext(missingids, new ObjectIDSet(), true);

    this.testFaultSinkContext.resetCounter();
    this.objectManager.lookupObjectsAndSubObjectsFor(null, result2, -1);
    this.testFaultSinkContext.waitUntillCounterIs(2);
    assertEquals(missingids, result2.missing);

    // Now release the first two objects
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, result1.objects.values());

    // Counter shouldn't be incremented, in other words, missing objects should not be looked up again.
    assertEquals(2, this.testFaultSinkContext.getCounter());
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
    TestArrayDNA ta;
    mo.apply((ta = new TestArrayDNA(id1)), new TransactionID(1), new ApplyTransactionInfo(), imo, false);
    mo = results.objects.get(id2);
    mo.apply(new TestArrayDNA(id2), new TransactionID(2), new ApplyTransactionInfo(), imo, false);

    Map ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(2, ic.get(ta.getTypeName()));

    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

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
    for (final Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      final ObjectID id = iter.next();
      mo = results.objects.get(id);
      if (newIDs.contains(id)) {
        mo.apply(new TestArrayDNA(id), new TransactionID(count++), new ApplyTransactionInfo(), imo, false);
      } else {
        mo.apply(new TestArrayDNA(id, true), new TransactionID(count++), new ApplyTransactionInfo(), imo, false);

      }
    }
    ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(4, ic.get(ta.getTypeName()));

    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());
  }

  public void testArrayFacade() throws Exception {
    initObjectManager();

    final ObjectID id = new ObjectID(1);
    final ObjectIDSet ids = new ObjectIDSet();
    ids.add(id);
    this.objectManager.createNewObjects(ids);
    final TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    final ManagedObject mo = lookedUpObjects.get(id);
    mo.apply(new TestArrayDNA(id), new TransactionID(1), new ApplyTransactionInfo(), imo, false);
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, lookedUpObjects.values());

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

    final ObjectID dateID = new ObjectID(1);

    final ObjectIDSet ids = new ObjectIDSet();
    ids.add(dateID);
    this.objectManager.createNewObjects(ids);
    final TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    final ManagedObject dateManagedObject = lookedUpObjects.get(dateID);

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    dateManagedObject.apply(new TestDateDNA("java.util.Date", dateID), new TransactionID(1),
                            new ApplyTransactionInfo(), imo, false);

    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(dateID, 1);
    validateDateFacade(facade);

  }

  public void testLiteralFacades() throws NoSuchObjectException {
    initObjectManager();

    final ObjectID literalID = new ObjectID(1);

    final ObjectIDSet ids = new ObjectIDSet();
    ids.add(literalID);

    this.objectManager.createNewObjects(ids);
    final TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    final ManagedObject managedObject = lookedUpObjects.get(literalID);

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    managedObject.apply(new TestLiteralValuesDNA(literalID), new TransactionID(1), new ApplyTransactionInfo(), imo,
                        false);

    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = this.objectManager.lookupFacade(literalID, 1);
    validateLiteralFacade(facade);

  }

  private void validateLiteralFacade(final ManagedObjectFacade literalFacade) {
    assertFalse(literalFacade.isArray());
    assertFalse(literalFacade.isMap());
    assertFalse(literalFacade.isSet());
    assertFalse(literalFacade.isList());
    assertEquals("java.lang.Integer", literalFacade.getClassName());

    final Object value = literalFacade.getFieldValue("java.lang.Integer");

    assertTrue(value instanceof Integer);
  }

  public void testLogicalFacades() throws NoSuchObjectException {
    initObjectManager();

    final ObjectID mapID = new ObjectID(1);
    final ObjectID listID = new ObjectID(2);
    final ObjectID setID = new ObjectID(3);

    final ObjectIDSet ids = new ObjectIDSet();
    ids.add(mapID);
    ids.add(listID);
    ids.add(setID);

    this.objectManager.createNewObjects(ids);
    final TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map<ObjectID, ManagedObject> lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    final ManagedObject list = lookedUpObjects.get(listID);
    final ManagedObject set = lookedUpObjects.get(setID);
    final ManagedObject map = lookedUpObjects.get(mapID);

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    map.apply(new TestMapDNA(mapID), new TransactionID(1), new ApplyTransactionInfo(), imo, false);
    set.apply(new TestListSetDNA("java.util.HashSet", setID), new TransactionID(1), new ApplyTransactionInfo(), imo,
              false);
    list.apply(new TestListSetDNA("java.util.LinkedList", listID), new TransactionID(1), new ApplyTransactionInfo(),
               imo, false);

    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, lookedUpObjects.values());

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

  private void validateListFacade(final ManagedObjectFacade listFacade, final int facadeSize, final int totalSize) {
    assertFalse(listFacade.isArray());
    assertFalse(listFacade.isMap());
    assertFalse(listFacade.isSet());
    assertTrue(listFacade.isList());
    assertEquals("java.util.LinkedList", listFacade.getClassName());
    assertEquals(facadeSize, listFacade.getFacadeSize());
    assertEquals(totalSize, listFacade.getTrueObjectSize());

    for (int i = 0; i < facadeSize; i++) {
      final String fName = String.valueOf(i);
      final Object value = listFacade.getFieldValue(fName);
      assertTrue(value instanceof String);
      assertEquals("item" + (i + 1), value);
    }
  }

  private void validateSetFacade(final ManagedObjectFacade setFacade, final int facadeSize, final int totalSize) {
    assertFalse(setFacade.isArray());
    assertFalse(setFacade.isMap());
    assertTrue(setFacade.isSet());
    assertFalse(setFacade.isList());
    assertEquals("java.util.HashSet", setFacade.getClassName());
    assertEquals(facadeSize, setFacade.getFacadeSize());
    assertEquals(totalSize, setFacade.getTrueObjectSize());

    final Set<String> expect = new HashSet<String>();
    expect.add("item1");
    expect.add("item2");
    expect.add("item3");

    final Set<String> actual = new HashSet<String>();
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
    assertEquals("java.util.HashMap", mapFacade.getClassName());
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

  private void validateDateFacade(final ManagedObjectFacade dateFacade) {
    assertFalse(dateFacade.isArray());
    assertFalse(dateFacade.isMap());
    assertFalse(dateFacade.isSet());
    assertFalse(dateFacade.isList());
    assertEquals("java.util.Date", dateFacade.getClassName());

    final Object value = dateFacade.getFieldValue("date");

    assertTrue(value instanceof Date);
  }

  private BerkeleyDBEnvironment newDBEnvironment(final boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new BerkeleyDBEnvironment(paranoid, dbHome);
  }

  private Persistor newPersistor(final BerkeleyDBEnvironment dbEnv,
                                 final SerializationAdapterFactory serializationAdapterFactory) throws Exception {
    return new DBPersistorImpl(this.logger, dbEnv, serializationAdapterFactory);
  }

  private SerializationAdapterFactory newSleepycatSerializationAdapterFactory(final BerkeleyDBEnvironment dbEnv) {
    return new CustomSerializationAdapterFactory();
    // return new SleepycatSerializationAdapterFactory(dbEnv);
  }

  private SerializationAdapterFactory newCustomSerializationAdapterFactory() {
    return new CustomSerializationAdapterFactory();
  }

  public void testLookupInPersistentContext() throws Exception {
    boolean paranoid = false;
    // sleepycat serializer, not paranoid
    BerkeleyDBEnvironment dbEnv = newDBEnvironment(paranoid);
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

  private void testLookupInPersistentContext(final Persistor persistor, final boolean paranoid,
                                             final PersistenceTransactionProvider ptp) throws Exception {
    final ManagedObjectPersistor mop = persistor.getManagedObjectPersistor();
    final PersistentManagedObjectStore store = new PersistentManagedObjectStore(mop, new MockSink());
    final TestSink faultSink = new TestSink();
    final TestSink flushSink = new TestSink();
    this.config.paranoid = paranoid;
    this.objectManager = new ObjectManagerImpl(this.config, this.clientStateManager, store, new LRUEvictionPolicy(100),
                                               this.persistenceTransactionProvider, faultSink, flushSink,
                                               this.objectStatsRecorder);
    new TestMOFaulter(this.objectManager, store, faultSink, new NullSinkContext()).start();
    new TestMOFlusher(this.objectManager, flushSink, new NullSinkContext()).start();

    final ObjectID id = new ObjectID(1);
    final ObjectIDSet ids = new ObjectIDSet();
    ids.add(id);
    final ClientID key = new ClientID(0);

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

    final DNACursor cursor = new DNACursor() {
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

      public boolean next(final DNAEncoding encoding) {
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

    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), imo,
                                                       false);

    PersistenceTransaction tx = ptp.newTransaction();
    this.objectManager.releaseAndCommit(tx, lookedUpViaLookupObjectsForCreateIfNecessary);

    ManagedObject lookedUpViaLookup = this.objectManager.getObjectByID(id);
    assertEquals(1, lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().size());
    assertEquals(lookedUpViaLookup.getObjectReferences(),
                 lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences());

    tx = ptp.newTransaction();
    this.objectManager.releaseAndCommit(tx, lookedUpViaLookup);

    // now do another lookup, change, and commit cycle
    responseContext = new TestResultsContext(ids, new ObjectIDSet());
    lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(key, responseContext);
    lookedUpViaLookupObjectsForCreateIfNecessary = lookedUpObjects.get(id);
    countSlot.set(0, 1);
    final ObjectID newReferenceID = new ObjectID(9324);
    fieldValueSlot.set(0, newReferenceID);
    dna = new TestDNA(cursor);
    dna.version = 10;
    dna.isDelta = true;
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(2), new ApplyTransactionInfo(), imo,
                                                       false);
    // lookedUpViaLookupObjectsForCreateIfNecessary.commit();
    tx = ptp.newTransaction();
    this.objectManager.releaseAndCommit(tx, lookedUpViaLookupObjectsForCreateIfNecessary);

    lookedUpViaLookup = this.objectManager.getObjectByID(id);
    assertEquals(1, lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().size());
    assertTrue(lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().contains(newReferenceID));

    assertEquals(lookedUpViaLookup.getObjectReferences(),
                 lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences());

    close(persistor, store);
  }

  private static void close(final Persistor persistor, final PersistentManagedObjectStore store) {
    // to work around timing problem with this test, calling snapshot
    // this should block this thread until transaction reading all object IDs from BDB completes,
    // at which point, it's OK to close the DB
    persistor.getManagedObjectPersistor().snapshotObjectIDs();
    persistor.getManagedObjectPersistor().snapshotEvictableObjectIDs();
    persistor.getManagedObjectPersistor().snapshotMapTypeObjectIDs();
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
    this.objectManager.releaseAndCommit(this.NULL_TRANSACTION, mo);
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
    this.objectManager.releaseAndCommit(this.NULL_TRANSACTION, mo);
    ThreadUtil.reallySleep(1000);
    assertTrue(gotIt[0]);
  }

  public void testPhysicalObjectFacade() throws Exception {
    testPhysicalObjectFacade(false);
    testPhysicalObjectFacade(true);
  }

  private void testPhysicalObjectFacade(final boolean paranoid) throws Exception {
    final BerkeleyDBEnvironment dbEnv = newDBEnvironment(paranoid);
    final SerializationAdapterFactory saf = newCustomSerializationAdapterFactory();
    final Persistor persistor = newPersistor(dbEnv, saf);
    final PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    final PersistentManagedObjectStore persistantMOStore = new PersistentManagedObjectStore(
                                                                                            persistor
                                                                                                .getManagedObjectPersistor(),
                                                                                            new MockSink());
    this.objectStore = persistantMOStore;
    this.config.paranoid = paranoid;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), new NullCache(),
                      this.objectStore);

    final ObjectIDSet oids = new ObjectIDSet();
    oids.add(new ObjectID(1));

    this.objectManager.createNewObjects(oids);
    final TestResultsContext context = new TestResultsContext(oids, oids);
    this.objectManager.lookupObjectsFor(null, context);
    context.waitTillComplete();
    final ManagedObject mo = (context.objects).get(new ObjectID(1));
    assertTrue(mo.isNew());
    final ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    mo.apply(new TestPhysicalDNA(new ObjectID(1)), new TransactionID(1), new ApplyTransactionInfo(), imo, false);

    final PersistenceTransaction tx = ptp.newTransaction();
    this.objectManager.releaseAndCommit(tx, mo);
    if (!paranoid) {
      // Object manager doesn't commit if in non-paranoid mode.
      tx.commit();
    }

    ManagedObjectFacade facade;
    try {
      facade = this.objectManager.lookupFacade(new ObjectID(1), -1);
    } catch (final NoSuchObjectException e1) {
      fail(e1.getMessage());
      return;
    }

    final String[] fieldNames = facade.getFields();
    assertEquals(6, fieldNames.length);
    // NOTE: the order of the object fields should be alphabetic
    assertTrue(Arrays.asList(fieldNames).toString(),
               Arrays.equals(fieldNames, new String[] { "access$0", "this$0", "intField", "objField", "stringField",
                   "zzzField" }));
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
    } catch (final IllegalArgumentException iae) {
      // expected
    }

    try {
      facade.getFieldValue("does not exist");
      fail();
    } catch (final IllegalArgumentException iae) {
      // expected
    }

    try {
      facade.isPrimitive("does not exist");
      fail();
    } catch (final IllegalArgumentException iae) {
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
    this.objectManager.releaseAndCommit(this.NULL_TRANSACTION, mo);
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
    this.objectManager.releaseAndCommit(this.NULL_TRANSACTION, mo);
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
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());

    assertEquals(10, this.stats.getTotalRequests());
    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(10, this.stats.getTotalCacheMisses());

    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(20, this.stats.getTotalRequests());
    assertEquals(10, this.stats.getTotalCacheHits());
    assertEquals(10, this.stats.getTotalCacheMisses());

    ids = makeObjectIDSet(10, 20);
    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(30, this.stats.getTotalRequests());
    assertEquals(10, this.stats.getTotalCacheHits());
    assertEquals(20, this.stats.getTotalCacheMisses());

    evictCache(10);

    ids = makeObjectIDSet(14, 4);
    results = new TestResultsContext(ids, new ObjectIDSet());
    this.objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    this.objectManager.releaseAllAndCommit(this.NULL_TRANSACTION, results.objects.values());
    assertEquals(40, this.stats.getTotalRequests());
    assertEquals(15, this.stats.getTotalCacheHits());
    assertEquals(25, this.stats.getTotalCacheMisses());

    final double hitRate = ((double) 15) / ((double) 40);
    assertEquals(hitRate, this.stats.getCacheHitRatio(), 0D);
  }

  private void evictCache(final int inCache) {
    final TestCacheStats tc = new TestCacheStats();
    tc.toKeep = inCache;
    this.objectManager.evictCache(tc);
    tc.validate();
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

  private void createObjects(final int num, final int inCache) {
    createObjects(num);
    evictCache(inCache);
  }

  private Set<ObjectID> createObjects(final int num) {
    return createObjects(0, num, new HashSet());
  }

  public Set<ObjectID> createObjects(final int startID, final int endID, final Set<ObjectID> children) {
    return createObjects(startID, endID, children, new HashSet());
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
      this.objectStore.addNewObject(mo);
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

  /**
   * DEV-5113
   */
  public void testEvictCacheAndGCRunParallel() throws Exception {
    this.config.paranoid = false;
    TestMOFlusherWithLatch flushWithLatch = initObjectManagerAndGetFlusher(createThreadGroup(),
                                                                           new LRUEvictionPolicy(-1));
    this.config.myGCThreadSleepTime = -1;
    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    ArrayList<ManagedObject> managedObjects = new ArrayList<ManagedObject>();
    ObjectIDSet objectIDset = new ObjectIDSet();
    for (int i = 0; i < 10000; i++) {
      ObjectID id = new ObjectID(i);
      ManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>(3));

      mo.setIsDirty(true);
      this.objectManager.createObject(mo);

      managedObjects.add(mo);
      objectIDset.add(id);
    }

    Thread evictor = new Thread() {
      @Override
      public void run() {
        objectManager.evictCache(new CacheStats() {
          public void objectEvicted(int evictedCount, int currentCount, List targetObjects4GC, boolean printNewObjects) {
            //
          }

          public int getObjectCountToEvict(int currentCount) {
            return 10000;
          }
        });

      }
    };
    evictor.start();

    ThreadUtil.reallySleep(5000);

    final Thread gcCaller = new Thread(new GCCaller(), "GCCaller");
    gcCaller.start();
    gc.collectedObjects = objectIDset;

    // FLUSHER and the EVICTOR are running in parallel now. High chance of race in removing the references
    flushWithLatch.getLatch().release();
    gc.allow_blockUntilReadyToGC_ToProceed();

    assertTrue(gc.waitFor_notifyGCComplete_ToBeCalled(150000));
    gcCaller.join();
    evictor.join();
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
    this.objectManager.releaseAndCommit(this.NULL_TRANSACTION, mo);

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
    final BerkeleyDBEnvironment dbEnv = newDBEnvironment(true);
    final SerializationAdapterFactory saf = newCustomSerializationAdapterFactory();
    final Persistor persistor = newPersistor(dbEnv, saf);
    final PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    final PersistentManagedObjectStore persistentMOStore = new PersistentManagedObjectStore(
                                                                                            persistor
                                                                                                .getManagedObjectPersistor(),
                                                                                            new MockSink());
    this.objectStore = persistentMOStore;
    this.config.paranoid = true;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), new NullCache(),
                      this.objectStore);
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

    changes.put(new ObjectID(1), new TestPhysicalDNA(new ObjectID(1)));

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
    ApplyCompleteEventContext acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    final CommitTransactionContext ctc1 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
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
    changes.put(new ObjectID(1), new TestPhysicalDNA(new ObjectID(1), true));
    changes.put(new ObjectID(2), new TestPhysicalDNA(new ObjectID(2), true));
    changes.put(new ObjectID(3), new TestPhysicalDNA(new ObjectID(3)));

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
    acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    final CommitTransactionContext ctc2 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
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
    this.objectManager.releaseAllAndCommit(dbtxn, ctc1.getObjects());

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
    dbtxn = ptp.newTransaction();
    this.objectManager.releaseAllAndCommit(dbtxn, ctc2.getObjects());

    cb.await();

    assertTrue(gc.isPaused());

    // Complete gc
    gc.deleteGarbage(new PeriodicDGCResultContext(TCCollections.EMPTY_OBJECT_ID_SET, new GarbageCollectionInfo()));

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
    acec = (ApplyCompleteEventContext) this.coordinator.applyCompleteSink.queue.take();
    assertNotNull(acec);
    assertTrue(this.coordinator.applyCompleteSink.queue.isEmpty());

    this.txObjectManager.processApplyComplete();
    final CommitTransactionContext ctc3 = (CommitTransactionContext) this.coordinator.commitSink.queue.take();
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
    this.objectManager.releaseAllAndCommit(dbtxn, ctc3.getObjects());

    assertEquals(0, this.objectManager.getCheckedOutCount());
    assertFalse(this.objectManager.isReferenced(new ObjectID(1)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(2)));
    assertFalse(this.objectManager.isReferenced(new ObjectID(3)));

    close(persistor, persistentMOStore);
  }

  public void testGetObjectReferencesFrom() {
    this.config.paranoid = true;
    final TestEvictionPolicy policy = new TestEvictionPolicy();
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), policy);
    this.objectManager.setStatsListener(this.stats);

    final TestGarbageCollector gc = new TestGarbageCollector(this.objectManager);
    this.objectManager.setGarbageCollector(gc);
    this.objectManager.start();

    assertEquals(0, this.stats.getTotalCacheHits());
    assertEquals(0, this.stats.getTotalCacheMisses());

    // each object has 1000 distinct reachable objects
    final Set<ObjectID> children = createObjects(1000, 2000, new HashSet<ObjectID>());

    createObjects(0, 2, children);
    final Set<TestManagedObject> objects = new HashSet();

    createObjects(3, 5, children, objects);
    final Set<ObjectID> returnedSet = this.objectManager.getObjectReferencesFrom(new ObjectID(1), false);
    assertEquals(1000, returnedSet.size());

    // ObjectManager should give out objects references counts to DGC even if its passed
    gc.requestGCPause();

    final Set<ObjectID> returnedCachedNoReapSet = this.objectManager.getObjectReferencesFrom(new ObjectID(4), true);
    assertEquals(1000, returnedCachedNoReapSet.size());

    gc.notifyReadyToGC();
    gc.notifyGCComplete();

    policy.objects = objects;
    final CacheStatsYoungGC cacheStats = new CacheStatsYoungGC();
    this.objectManager.evictCache(cacheStats);

    final Set<ObjectID> returnedCachedReapSet = this.objectManager.getObjectReferencesFrom(new ObjectID(4), true);
    assertEquals(0, returnedCachedReapSet.size());

  }

  public static class CacheStatsYoungGC implements CacheStats {

    public int getObjectCountToEvict(final int currentCount) {
      return 2;
    }

    public void objectEvicted(final int evictedCount, final int currentCount, final List targetObjects4GC,
                              final boolean printNewObjects) {
      //
    }

  }

  private static class TestEvictionPolicy implements EvictionPolicy {

    public Set<TestManagedObject> objects;

    public boolean add(final Cacheable obj) {
      return false;
    }

    public int getCacheCapacity() {
      return 0;
    }

    public Collection getRemovalCandidates(final int maxCount) {
      return this.objects;
    }

    public void markReferenced(final Cacheable obj) {
      //
    }

    public void remove(final Cacheable obj) {
      //
    }

    public PrettyPrinter prettyPrint(final PrettyPrinter out) {
      return new PrettyPrinterImpl(new PrintWriter(new StringWriter()));
    }

  }

  private ApplyTransactionInfo applyTxn(final ApplyTransactionContext aoc) {
    final ServerTransaction txn = aoc.getTxn();
    final Map managedObjects = aoc.getObjects();
    final ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    final ApplyTransactionInfo applyTxnInfo = new ApplyTransactionInfo(txn.isActiveTxn(), txn.getServerTransactionID());
    for (final Iterator i = txn.getChanges().iterator(); i.hasNext();) {
      final DNA dna = (DNA) i.next();
      final ManagedObject mo = (ManagedObject) managedObjects.get(dna.getObjectID());
      mo.apply(new VersionizedDNAWrapper(dna, ++this.version), txn.getTransactionID(), applyTxnInfo, instanceMonitor,
               false);
    }
    return applyTxnInfo;
  }

  private static class TestArrayDNA implements DNA {

    private static int     _version;

    private final ObjectID id;
    private final int      version;

    private final boolean  delta;

    public TestArrayDNA(final ObjectID id) {
      this(id, false);
    }

    public TestArrayDNA(final ObjectID id, final boolean delta) {
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

        public boolean next(final DNAEncoding encoding) {
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

    public TestListSetDNA(final String className, final ObjectID setID) {
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
              final Object item = new UTF8ByteDataHolder("item" + this.count);
              return new LogicalAction(SerializationUtil.ADD, new Object[] { item });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(final DNAEncoding encoding) {
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

    TestMapDNA(final ObjectID id) {
      this(id, false);
    }

    TestMapDNA(final ObjectID id, final boolean isDelta) {
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
              final Object key = new UTF8ByteDataHolder("key" + this.count);
              final Object val = new UTF8ByteDataHolder("val" + this.count);
              return new LogicalAction(SerializationUtil.PUT, new Object[] { key, val });
            default:
              throw new RuntimeException("bad count: " + this.count);
          }
        }

        public PhysicalAction getPhysicalAction() {
          throw new ImplementMe();
        }

        public boolean next(final DNAEncoding encoding) {
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

  public static class TestDateDNA implements DNA {

    final ObjectID setID;
    final String   className;

    public TestDateDNA(final String className, final ObjectID setID) {
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

        public boolean next(final DNAEncoding encoding) {
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

    public TestResultsContext(final ObjectIDSet ids, final ObjectIDSet newIDS, final boolean updateStats) {
      this.ids = ids;
      this.newIDS = newIDS;
      this.updateStats = updateStats;
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

    public synchronized void setResults(final ObjectManagerLookupResults results) {
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

    TestPhysicalDNA(final ObjectID id) {
      this(id, false);
    }

    public TestPhysicalDNA(final ObjectID id, final boolean isDelta) {
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

        public boolean next(final DNAEncoding encoding) {
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

    TestLiteralValuesDNA(final ObjectID id) {
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

        public boolean next(final DNAEncoding encoding) {
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
      super(10000, true, true, true, false, 60000, 1000);
    }

    TestObjectManagerConfig(final long gcThreadSleepTime, final boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, false, 60000, 1000);
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

    public TestMOFaulter(final ObjectManagerImpl objectManager, final ManagedObjectStore store,
                         final TestSink faultSink, final SinkContext sinkContext) {
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
          final ManagedObjectFaultingContext ec = (ManagedObjectFaultingContext) this.faultSink.take();
          this.objectManager.addFaultedObject(ec.getId(), this.store.getObjectByID(ec.getId()), ec.isRemoveOnRelease());
          this.sinkContext.postProcess();

        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class TestMOFlusher extends Thread {

    final ObjectManagerImpl objectManager;
    final TestSink          flushSink;
    final SinkContext       sinkContext;

    public TestMOFlusher(final ObjectManagerImpl objectManager, final TestSink flushSink, final SinkContext sinkContext) {
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
          final ManagedObjectFlushingContext ec = (ManagedObjectFlushingContext) this.flushSink.take();
          this.objectManager.flushAndEvict(ec.getObjectToFlush());
          this.sinkContext.postProcess();
        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class TestMOFlusherWithLatch extends TestMOFlusher {

    private final Latch latch = new Latch();

    public TestMOFlusherWithLatch(final ObjectManagerImpl objectManager, final TestSink flushSink,
                                  final SinkContext sinkContext) {
      super(objectManager, flushSink, sinkContext);
    }

    public Latch getLatch() {
      return latch;
    }

    @Override
    public void run() {
      try {
        latch.acquire();
      } catch (InterruptedException e1) {
        throw new AssertionError(e1);
      }
      while (true) {
        try {
          final ManagedObjectFlushingContext ec = (ManagedObjectFlushingContext) this.flushSink.take();
          this.objectManager.flushAndEvict(ec.getObjectToFlush());
          this.sinkContext.postProcess();
        } catch (final InterruptedException e) {
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

    public void waitUntillCounterIs(final int count) {
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
