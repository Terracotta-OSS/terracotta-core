/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.ImplementMe;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.object.MockObjectManagementMonitor;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.LRUEvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.cache.TestCacheStats;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.context.ManagedObjectFaultingContext;
import com.tc.objectserver.context.ManagedObjectFlushingContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.core.api.Filter;
import com.tc.objectserver.core.api.GarbageCollector;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.core.impl.TestManagedObject;
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
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.objectserver.persistence.impl.TestPersistenceTransaction;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.LifeCycleState;
import com.tc.util.concurrent.StoppableThread;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

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

  /**
   * Constructor for ObjectManagerTest.
   * 
   * @param arg0
   */
  public ObjectManagerTest(String arg0) {
    super(arg0);
  }

  protected void setUp() throws Exception {
    super.setUp();
    this.logger = TCLogging.getLogger(getClass());
    this.managed = new HashMap();
    config = new TestObjectManagerConfig();
    clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());
    this.newObjectCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    this.objectfaultCounter = new SampledCounterImpl(new SampledCounterConfig(1, 1, true, 0L));
    stats = new ObjectManagerStatsImpl(newObjectCounter, objectfaultCounter);
    persistenceTransactionProvider = new TestPersistenceTransactionProvider();
    NULL_TRANSACTION = TestPersistenceTransaction.NULL_TRANSACTION;
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
    try {
      this.objectManager = new ObjectManagerImpl(config, threadGroup, clientStateManager, store, cache,
                                                 persistenceTransactionProvider, faultSink, flushSink,
                                                 new MockObjectManagementMonitor());
    } catch (NotCompliantMBeanException e) {
      throw new RuntimeException(e);
    }
    new TestMOFaulter(this.objectManager, store, faultSink).start();
    new TestMOFlusher(this.objectManager, flushSink).start();
  }

  public void testShutdownAndSetGarbageCollector() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.setGarbageCollector(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookup() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.getObjectByID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok;
    }
  }

  public void testShutdownAndLookupRootID() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.lookupRootID(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateRoot() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.createRoot(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndCreateObject() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.createObject(null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndGetRoots() throws Exception {
    initObjectManager();
    objectManager.stop();
    try {
      objectManager.getRoots();
      fail("Should have thrown a ShutdownError");
    } catch (ShutdownError e) {
      // ok.
    }

  }

  public void testShutdownAndLookupObjectsForCreateIfNecessary() throws Exception {
    initObjectManager();

    objectManager.stop();

    try {
      objectManager.lookupObjectsFor(null, null);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testShutdownAndLookupObjectsFor() throws Exception {
    initObjectManager();

    objectManager.stop();

    try {
      objectManager.lookupObjectsAndSubObjectsFor(null, null, -1);
      fail("Should have thrown a ShutdownError.");
    } catch (ShutdownError e) {
      // ok.
    }
  }

  public void testNewObjectIDs() {
    // this test is to make sure that the list of newly created objects IDs is
    // accurate in the lookup results
    initObjectManager();

    Set ids = new HashSet(); // important to use a Set here

    ObjectID id1;
    ids.add((id1 = new ObjectID(1)));
    ObjectID id2;
    ids.add((id2 = new ObjectID(2)));
    ChannelID key = new ChannelID(0);

    TestResultsContext results = new TestResultsContext(ids, ids);
    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(2, results.objects.size());

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();

    ManagedObject mo = (ManagedObject) results.objects.get(id1);
    TestArrayDNA ta;
    mo.apply((ta = new TestArrayDNA(id1)), new TransactionID(1), new BackReferences(), imo);
    mo = (ManagedObject) results.objects.get(id2);
    mo.apply(new TestArrayDNA(id2), new TransactionID(2), new BackReferences(), imo);

    Map ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(new Integer(2), ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());

    ids.add(new ObjectID(3));
    ids.add(new ObjectID(4));
    Set newIDs = new HashSet();
    newIDs.add(new ObjectID(3));
    newIDs.add(new ObjectID(4));

    results = new TestResultsContext(ids, newIDs);

    this.objectManager.lookupObjectsFor(key, results);
    assertEquals(4, results.objects.size());

    int count = 100;
    for (Iterator i = ids.iterator(); i.hasNext();) {
      ObjectID id = (ObjectID) i.next();
      mo = (ManagedObject) results.objects.get(id);
      mo.apply(new TestArrayDNA(id), new TransactionID(count++), new BackReferences(), imo);
    }
    ic = imo.getInstanceCounts();
    assertEquals(1, ic.size());
    assertEquals(new Integer(4), ic.get(ta.getTypeName()));

    this.objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());
  }

  public void testArrayFacade() throws Exception {
    initObjectManager();

    ObjectID id = new ObjectID(1);
    HashSet ids = new HashSet();
    ids.add(id);

    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    ManagedObject mo = (ManagedObject) lookedUpObjects.get(id);
    mo.apply(new TestArrayDNA(id), new TransactionID(1), new BackReferences(), imo);
    objectManager.releaseAll(NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = objectManager.lookupFacade(id, -1);
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
    facade = objectManager.lookupFacade(id, 1);
    assertEquals(1, facade.getArrayLength());
    assertEquals("tim", facade.getFieldValue("0"));

    facade = objectManager.lookupFacade(id, 19212);
    assertEquals(3, facade.getArrayLength());
    assertEquals("tim", facade.getFieldValue("0"));
    assertEquals("is", facade.getFieldValue("1"));
    assertEquals("here", facade.getFieldValue("2"));
  }

  public void testDateFacades() throws NoSuchObjectException {
    initObjectManager();

    ObjectID dateID = new ObjectID(1);

    Set ids = new HashSet();
    ids.add(dateID);

    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject dateManagedObject = (ManagedObject) lookedUpObjects.get(dateID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    dateManagedObject.apply(new TestDateDNA("java.util.Date", dateID), new TransactionID(1), new BackReferences(), imo);

    objectManager.releaseAll(NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = objectManager.lookupFacade(dateID, 1);
    validateDateFacade(facade);

  }

  public void testLiteralFacades() throws NoSuchObjectException {
    initObjectManager();

    ObjectID literalID = new ObjectID(1);

    Set ids = new HashSet();
    ids.add(literalID);

    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject managedObject = (ManagedObject) lookedUpObjects.get(literalID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    managedObject.apply(new TestLiteralValuesDNA(literalID), new TransactionID(1), new BackReferences(), imo);

    objectManager.releaseAll(NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = objectManager.lookupFacade(literalID, 1);
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

    Set ids = new HashSet();
    ids.add(mapID);
    ids.add(listID);
    ids.add(setID);

    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    final Map lookedUpObjects = responseContext.objects;

    this.objectManager.lookupObjectsFor(null, responseContext);
    assertEquals(ids.size(), lookedUpObjects.size());

    ManagedObject list = (ManagedObject) lookedUpObjects.get(listID);
    ManagedObject set = (ManagedObject) lookedUpObjects.get(setID);
    ManagedObject map = (ManagedObject) lookedUpObjects.get(mapID);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    map.apply(new TestMapDNA(mapID), new TransactionID(1), new BackReferences(), imo);
    set.apply(new TestListSetDNA("java.util.HashSet", setID), new TransactionID(1), new BackReferences(), imo);
    list.apply(new TestListSetDNA("java.util.LinkedList", listID), new TransactionID(1), new BackReferences(), imo);

    objectManager.releaseAll(NULL_TRANSACTION, lookedUpObjects.values());

    ManagedObjectFacade facade;

    facade = objectManager.lookupFacade(mapID, -1);
    validateMapFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(mapID, 5);
    validateMapFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(mapID, 1);
    validateMapFacade(facade, 1, 3);
    facade = objectManager.lookupFacade(mapID, 0);
    validateMapFacade(facade, 0, 3);

    facade = objectManager.lookupFacade(setID, -1);
    validateSetFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(setID, 5);
    validateSetFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(setID, 1);
    validateSetFacade(facade, 1, 3);
    facade = objectManager.lookupFacade(setID, 0);
    validateSetFacade(facade, 0, 3);

    facade = objectManager.lookupFacade(listID, -1);
    validateListFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(listID, 5);
    validateListFacade(facade, 3, 3);
    facade = objectManager.lookupFacade(listID, 1);
    validateListFacade(facade, 1, 3);
    facade = objectManager.lookupFacade(listID, 0);
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

    Set expect = new HashSet();
    expect.add("item1");
    expect.add("item2");
    expect.add("item3");

    Set actual = new HashSet();
    for (int i = 0; i < facadeSize; i++) {
      String fName = String.valueOf(i);
      Object value = setFacade.getFieldValue(fName);
      assertTrue(value instanceof String);
      actual.add(value);
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

    Map expect = new HashMap();
    expect.put("key1", "val1");
    expect.put("key2", "val2");
    expect.put("key3", "val3");

    Map actual = new HashMap();

    for (int i = 0; i < facadeSize; i++) {
      String fName = String.valueOf(i);
      Object value = mapFacade.getFieldValue(fName);
      assertTrue(value instanceof MapEntryFacade);
      MapEntryFacade entry = (MapEntryFacade) value;
      actual.put(entry.getKey(), entry.getValue());
    }

    for (Iterator iter = actual.keySet().iterator(); iter.hasNext();) {
      Object key = iter.next();
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
    DBEnvironment env = new DBEnvironment(paranoid, dbHome);
    return env;
  }

  private Persistor newPersistor(boolean paranoid, DBEnvironment dbEnv,
                                 SerializationAdapterFactory serializationAdapterFactory) throws Exception {
    Persistor persistor = new SleepycatPersistor(logger, dbEnv, serializationAdapterFactory);
    return persistor;
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
    Persistor persistor = newPersistor(paranoid, dbEnv, saf);

    testLookupInPersistentContext(persistor, paranoid);

    // custom serializer, not paranoid
    dbEnv = newDBEnvironment(paranoid);
    saf = newCustomSerializationAdapterFactory();
    persistor = newPersistor(paranoid, dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid);

    // sleepycat serializer, paranoid
    paranoid = true;
    dbEnv = newDBEnvironment(paranoid);
    saf = newSleepycatSerializationAdapterFactory(dbEnv);
    persistor = newPersistor(paranoid, dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid);

    // custom serializer, paranoid
    dbEnv = newDBEnvironment(paranoid);
    saf = newCustomSerializationAdapterFactory();
    persistor = newPersistor(paranoid, dbEnv, saf);
    testLookupInPersistentContext(persistor, paranoid);
  }

  private void testLookupInPersistentContext(Persistor persistor, boolean paranoid) throws Exception {
    ManagedObjectPersistor mop = persistor.getManagedObjectPersistor();
    PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    PersistentManagedObjectStore store = new PersistentManagedObjectStore(mop);
    TestSink faultSink = new TestSink();
    TestSink flushSink = new TestSink();
    config.paranoid = paranoid;
    objectManager = new ObjectManagerImpl(config, createThreadGroup(), clientStateManager, store,
                                          new LRUEvictionPolicy(100), persistenceTransactionProvider, faultSink,
                                          flushSink, new MockObjectManagementMonitor());
    new TestMOFaulter(this.objectManager, store, faultSink).start();
    new TestMOFlusher(this.objectManager, flushSink).start();

    ObjectID id = new ObjectID(1);
    Set ids = new HashSet();
    ids.add(id);
    ChannelID key = new ChannelID(0);

    TestResultsContext responseContext = new TestResultsContext(ids, ids);
    Map lookedUpObjects = responseContext.objects;

    objectManager.lookupObjectsFor(key, responseContext);

    ManagedObject lookedUpViaLookupObjectsForCreateIfNecessary = (ManagedObject) lookedUpObjects.get(id);

    final String fieldName = "myField";
    final List countSlot = new ArrayList(1);
    countSlot.add(new Integer(1));
    final List fieldValueSlot = new ArrayList(1);
    fieldValueSlot.add(new ObjectID(100));

    DNACursor cursor = new DNACursor() {
      public LogicalAction getLogicalAction() {
        return null;
      }

      public PhysicalAction getPhysicalAction() {
        return new PhysicalAction(fieldName, fieldValueSlot.get(0), true);
      }

      public boolean next() {
        int count = ((Integer) countSlot.get(0)).intValue();
        count--;
        countSlot.set(0, new Integer(count));
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
        countSlot.set(0, new Integer(1));
      }
    };

    DNA dna = new TestDNA(cursor);

    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(1), new BackReferences(), imo);

    PersistenceTransaction tx = ptp.newTransaction();
    objectManager.release(tx, lookedUpViaLookupObjectsForCreateIfNecessary);
    tx.commit();

    ManagedObject lookedUpViaLookup = objectManager.getObjectByID(id);
    assertEquals(1, lookedUpViaLookupObjectsForCreateIfNecessary.getObjectReferences().size());
    assertEquals(lookedUpViaLookup.getObjectReferences(), lookedUpViaLookupObjectsForCreateIfNecessary
        .getObjectReferences());

    tx = ptp.newTransaction();
    objectManager.release(tx, lookedUpViaLookup);
    tx.commit();

    // now do another lookup, change, and commit cycle
    responseContext = new TestResultsContext(ids, Collections.EMPTY_SET);
    lookedUpObjects = responseContext.objects;

    objectManager.lookupObjectsFor(key, responseContext);
    lookedUpViaLookupObjectsForCreateIfNecessary = (ManagedObject) lookedUpObjects.get(id);
    countSlot.set(0, new Integer(1));
    ObjectID newReferenceID = new ObjectID(9324);
    fieldValueSlot.set(0, newReferenceID);
    dna = new TestDNA(cursor);
    lookedUpViaLookupObjectsForCreateIfNecessary.apply(dna, new TransactionID(2), new BackReferences(), imo);
    // lookedUpViaLookupObjectsForCreateIfNecessary.commit();
    tx = ptp.newTransaction();
    objectManager.release(tx, lookedUpViaLookupObjectsForCreateIfNecessary);
    tx.commit();

    lookedUpViaLookup = objectManager.getObjectByID(id);
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

  public void testExplodingGarbageCollector() throws Exception {
    LinkedQueue exceptionQueue = new LinkedQueue();
    TestThreadGroup tg = new TestThreadGroup(exceptionQueue);
    initObjectManager(tg);
    RuntimeException toThrow = new RuntimeException();
    this.objectManager.setGarbageCollector(new ExplodingGarbageCollector(toThrow));
    this.objectManager.start();
    Object o = exceptionQueue.poll(30 * 1000);
    assertEquals(toThrow, o);
  }

  public void testObjectManagerBasics() {
    initObjectManager();
    final ObjectID id = new ObjectID(0);
    ManagedObject mo = new TestManagedObject(id, new ObjectID[0]);
    objectManager.createObject(mo);
    assertFalse(objectManager.isReferenced(id));
    ManagedObject mo2 = objectManager.getObjectByID(id);
    assertTrue(mo == mo2);
    assertTrue(objectManager.isReferenced(id));
    objectManager.release(NULL_TRANSACTION, mo);
    assertFalse(objectManager.isReferenced(id));

    objectManager.getObjectByID(id);

    final boolean[] gotIt = new boolean[1];
    gotIt[0] = false;

    Thread t = new Thread() {
      public void run() {
        objectManager.getObjectByID(id);
        gotIt[0] = true;
      }
    };

    t.start();
    ThreadUtil.reallySleep(1000);
    assertFalse(gotIt[0]);
    objectManager.release(NULL_TRANSACTION, mo);
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
    Persistor persistor = newPersistor(paranoid, dbEnv, saf);
    PersistenceTransactionProvider ptp = persistor.getPersistenceTransactionProvider();
    this.objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    this.config.paranoid = paranoid;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))), new NullCache(),
                      this.objectStore);

    HashSet oids = new HashSet();
    oids.add(new ObjectID(1));

    final TestResultsContext context = new TestResultsContext(oids, oids);
    this.objectManager.lookupObjectsFor(null, context);
    context.waitTillComplete();
    ManagedObject mo = (ManagedObject) (context.objects).get(new ObjectID(1));
    assertTrue(mo.isNew());
    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    mo.apply(new TestPhysicalDNA(new ObjectID(1)), new TransactionID(1), new BackReferences(), imo);

    PersistenceTransaction tx = ptp.newTransaction();
    this.objectManager.release(tx, mo);
    tx.commit();

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
    assertEquals(new Integer(42), facade.getFieldValue("intField"));
    assertEquals(new Byte((byte) 1), facade.getFieldValue("zzzField"));
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

    this.objectStore.shutdown();

    // XXX: change the object again, make sure the facade is "stable" (ie.
    // doesn't change)
  }

  public void testObjectManagerAsync() {
    initObjectManager();
    final ObjectID id = new ObjectID(0);
    final ObjectID id1 = new ObjectID(1);

    Set objectIDs = new HashSet();

    ManagedObject mo = new TestManagedObject(id, new ObjectID[0]);
    ManagedObject mo1 = new TestManagedObject(id1, new ObjectID[0]);
    objectManager.createObject(mo);
    objectManager.createObject(mo1);

    assertFalse(objectManager.isReferenced(id));

    objectIDs.add(id);

    TestObjectManagerResultsContext context;
    assertTrue(objectManager
        .lookupObjectsAndSubObjectsFor(null, context = new TestObjectManagerResultsContext(new HashMap(), objectIDs),
                                       -1));

    ManagedObject retrievedMo = (ManagedObject) context.getResults().values().iterator().next();
    assertTrue(mo == retrievedMo);
    assertTrue(objectManager.isReferenced(id));
    objectManager.release(NULL_TRANSACTION, mo);
    assertFalse(objectManager.isReferenced(id));

    objectManager.getObjectByID(id);

    objectIDs.add(id1);

    boolean notPending = objectManager
        .lookupObjectsAndSubObjectsFor(null, context = new TestObjectManagerResultsContext(new HashMap(), objectIDs),
                                       -1);
    assertFalse(notPending);
    assertEquals(0, context.getResults().size());
    objectManager.release(NULL_TRANSACTION, mo);
    assertEquals(objectIDs.size(), context.getResults().size());

    Collection objs = context.getResults().values();
    assertTrue(objs.contains(mo));
    assertTrue(objs.contains(mo1));
    assertTrue(objs.size() == 2);
  }

  public void testNewObjectCounter() {
    initObjectManager();
    objectManager.setStatsListener(stats);
    createObjects(666);
    assertEquals(666, stats.getTotalObjectsCreated());
    assertEquals(666, newObjectCounter.getValue());

    // roots count as "new" objects too
    objectManager.createRoot("root", new ObjectID(4444));
    assertEquals(667, stats.getTotalObjectsCreated());
    assertEquals(667, newObjectCounter.getValue());
  }

  public void testCacheStats() throws Exception {
    config.paranoid = true;
    initObjectManager(new TCThreadGroup(new ThrowableHandler(TCLogging.getTestingLogger(getClass()))),
                      new LRUEvictionPolicy(-1));
    objectManager.setStatsListener(this.stats);

    assertEquals(0, stats.getTotalRequests());
    assertEquals(0, stats.getTotalCacheHits());
    assertEquals(0, stats.getTotalCacheMisses());

    createObjects(50, 10);
    Set ids = makeObjectIDSet(0, 10);
    // ThreadUtil.reallySleep(5000);
    TestResultsContext results = new TestResultsContext(ids, Collections.EMPTY_SET);

    objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());

    assertEquals(10, stats.getTotalRequests());
    assertEquals(0, stats.getTotalCacheHits());
    assertEquals(10, stats.getTotalCacheMisses());

    results = new TestResultsContext(ids, Collections.EMPTY_SET);
    objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());
    assertEquals(20, stats.getTotalRequests());
    assertEquals(10, stats.getTotalCacheHits());
    assertEquals(10, stats.getTotalCacheMisses());

    ids = makeObjectIDSet(10, 20);
    results = new TestResultsContext(ids, Collections.EMPTY_SET);
    objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());
    assertEquals(30, stats.getTotalRequests());
    assertEquals(10, stats.getTotalCacheHits());
    assertEquals(20, stats.getTotalCacheMisses());

    evictCache(10);

    ids = makeObjectIDSet(14, 4);
    results = new TestResultsContext(ids, Collections.EMPTY_SET);
    objectManager.lookupObjectsAndSubObjectsFor(null, results, -1);
    results.waitTillComplete();
    objectManager.releaseAll(NULL_TRANSACTION, results.objects.values());
    assertEquals(40, stats.getTotalRequests());
    assertEquals(15, stats.getTotalCacheHits());
    assertEquals(25, stats.getTotalCacheMisses());

    double hitRate = ((double) 15) / ((double) 40);
    assertEquals(hitRate, stats.getCacheHitRatio(), 0D);
  }

  private void evictCache(int inCache) {
    TestCacheStats tc = new TestCacheStats();
    tc.toKeep = inCache;
    objectManager.evictCache(tc);
    tc.validate();
  }

  private void createObjects(int num, int inCache) {
    createObjects(num);
    evictCache(inCache);
  }

  private Set makeObjectIDSet(int begin, int end) {
    Set rv = new HashSet();

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
      TestManagedObject mo = new TestManagedObject(new ObjectID(i), new ObjectID[] {});
      objectManager.createObject(mo);
    }
  }

  public void testGCStats() {
    initObjectManager();

    // this should disable the internal gc thread, allowing us to control when
    // objMgr.gc() happens
    this.config.myGCThreadSleepTime = -1;

    GarbageCollector gc = new MarkAndSweepGarbageCollector(objectManager, clientStateManager, true);
    objectManager.setGarbageCollector(gc);

    Listener listener = new Listener();
    this.objectManager.addListener(listener);

    objectManager.createRoot("root-me", new ObjectID(0));
    ManagedObject root = new TestManagedObject(new ObjectID(0), new ObjectID[] { new ObjectID(1) });
    objectManager.createObject(root);

    TestManagedObject mo1 = new TestManagedObject(new ObjectID(1), new ObjectID[] { new ObjectID(2) });
    TestManagedObject mo2 = new TestManagedObject(new ObjectID(2), new ObjectID[] { new ObjectID(3) });
    TestManagedObject mo3 = new TestManagedObject(new ObjectID(3), new ObjectID[] {});
    objectManager.createObject(mo1);
    objectManager.createObject(mo2);
    objectManager.createObject(mo3);

    ChannelID cid1 = new ChannelID(1);
    clientStateManager.addReference(cid1, root.getID());
    clientStateManager.addReference(cid1, mo1.getID());
    clientStateManager.addReference(cid1, mo2.getID());
    clientStateManager.addReference(cid1, mo3.getID());

    assertEquals(0, objectManager.getGarbageCollectorStats().length);
    assertEquals(0, listener.gcEvents.size());

    long start = System.currentTimeMillis();

    objectManager.gc();

    assertEquals(1, objectManager.getGarbageCollectorStats().length);
    assertEquals(1, listener.gcEvents.size());

    GCStats stats1 = (GCStats) listener.gcEvents.get(0);
    final int firstIterationNumber = stats1.getIteration();
    assertSame(stats1, objectManager.getGarbageCollectorStats()[0]);
    assertTrue("external: " + start + ", reported: " + stats1.getStartTime(), stats1.getStartTime() >= start);
    assertTrue(String.valueOf(stats1.getElapsedTime()), stats1.getElapsedTime() >= 0);
    assertEquals(4, stats1.getBeginObjectCount());
    assertEquals(0, stats1.getCandidateGarbageCount());
    assertEquals(0, stats1.getActualGarbageCount());

    listener.gcEvents.clear();
    objectManager.gc();
    assertEquals(2, objectManager.getGarbageCollectorStats().length);
    assertEquals(1, listener.gcEvents.size());
    assertEquals(firstIterationNumber + 1, objectManager.getGarbageCollectorStats()[0].getIteration());

    listener.gcEvents.clear();
    Set removed = new HashSet();
    removed.add(mo3.getID());
    clientStateManager.removeReferences(cid1, removed);
    mo2.setReferences(new ObjectID[] {});
    objectManager.gc();
    assertEquals(3, objectManager.getGarbageCollectorStats().length);
    assertEquals(1, listener.gcEvents.size());
    GCStats stats3 = (GCStats) listener.gcEvents.get(0);
    assertEquals(4, stats3.getBeginObjectCount());
    assertEquals(1, stats3.getActualGarbageCount());
    assertEquals(1, stats3.getCandidateGarbageCount());
  }

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
    TestGarbageCollector gc = new TestGarbageCollector(objectManager);
    objectManager.setGarbageCollector(gc);
    final ObjectID id = new ObjectID(0);
    ManagedObject mo = new TestManagedObject(id, new ObjectID[3]);
    objectManager.createObject(mo);

    assertFalse(gc.isCollected());

    gc.allow_blockUntilReadyToGC_ToProceed();

    objectManager.gc();
    assertTrue(gc.isCollected());

    gc.reset();

    // call lookup to check out an object...
    objectManager.getObjectByID(id);

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
    objectManager.release(NULL_TRANSACTION, mo);

    // make sure release calls notifyReadyToGC
    assertTrue(gc.waitFor_notifyReadyToGC_ToBeCalled(5000));

    // unblock the caller...
    gc.allow_blockUntilReadyToGC_ToProceed();

    // make sure the object manager calls notifyGCComplete
    assertTrue(gc.waitFor_notifyGCComplete_ToBeCalled(5000));
    gcCaller.join();
  }

  private static class TestArrayDNA implements DNA {

    private final ObjectID id;

    public TestArrayDNA(ObjectID id) {
      this.id = id;
    }

    public long getVersion() {
      return 0;
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
      return id;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return ObjectID.NULL_ID;
    }

    public DNACursor getCursor() {
      return new DNACursor() {
        int count = 0;

        public boolean next() {
          count++;
          return count <= 2;
        }

        public LogicalAction getLogicalAction() {
          throw new ImplementMe();
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public PhysicalAction getPhysicalAction() {
          switch (count) {
            case 1:
              return new PhysicalAction(new String[] { "tim", "was", "here" });
            case 2:
              return new PhysicalAction(1, "is", false);
            default:
              throw new RuntimeException("bad count: " + count);
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
      return false;
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
          count++;
          return count <= 3;
        }

        public LogicalAction getLogicalAction() {
          switch (count) {
            case 1:
            case 2:
            case 3:
              Object item = new UTF8ByteDataHolder("item" + count);
              return new LogicalAction(SerializationUtil.ADD, new Object[] { item });
            default:
              throw new RuntimeException("bad count: " + count);
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

    final ObjectID objectID;

    TestMapDNA(ObjectID id) {
      this.objectID = id;
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
          count++;
          return count <= 3;
        }

        public LogicalAction getLogicalAction() {
          switch (count) {
            case 1:
            case 2:
            case 3:
              Object key = new UTF8ByteDataHolder("key" + count);
              Object val = new UTF8ByteDataHolder("val" + count);
              return new LogicalAction(SerializationUtil.PUT, new Object[] { key, val });
            default:
              throw new RuntimeException("bad count: " + count);
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
          count++;
          return count <= 1;
        }

        public LogicalAction getLogicalAction() {
          switch (count) {
            case 1:
              return new LogicalAction(SerializationUtil.SET_TIME,
                                       new Object[] { new Long(System.currentTimeMillis()) });
            default:
              throw new RuntimeException("bad count: " + count);
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
    public Map        objects  = new HashMap();
    boolean           complete = false;
    private final Set ids;
    private final Set newIDS;

    public TestResultsContext(Set ids, Set newIDS) {
      this.ids = ids;
      this.newIDS = newIDS;
    }

    public synchronized void waitTillComplete() {
      while (!complete) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }

    public synchronized void setResults(ObjectManagerLookupResults results) {
      complete = true;
      this.objects.putAll(results.getObjects());
      notifyAll();
    }

    public Set getLookupIDs() {
      return ids;
    }

    public Set getNewObjectIDs() {
      return newIDS;
    }

  }

  private static class TestPhysicalDNA implements DNA {
    private final ObjectID id;

    TestPhysicalDNA(ObjectID id) {
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
          count++;
          return count < 7;
        }

        public LogicalAction getLogicalAction() {
          return null;
        }

        public Object getAction() {
          throw new ImplementMe();
        }

        public PhysicalAction getPhysicalAction() {
          switch (count) {
            case 1: {
              return new PhysicalAction("intField", new Integer(42), false);
            }
            case 2: {
              return new PhysicalAction("zzzField", new Byte((byte) 1), false);
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
          count = 0;
        }

      };
    }

    public boolean isDelta() {
      return false;
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
          count++;
          return count < 2;
        }

        public LogicalAction getLogicalAction() {
          return null;
        }

        public Object getAction() {
          switch (count) {
            case 1: {
              return new LiteralAction(new Integer(42));
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

  private static class Listener implements ObjectManagerEventListener {
    final List gcEvents = new ArrayList();

    public void garbageCollectionComplete(GCStats stats) {
      gcEvents.add(stats);
    }
  }

  private class ExplodingGarbageCollector implements GarbageCollector {

    private final RuntimeException toThrow;
    private LifeCycleState         gcState;

    public ExplodingGarbageCollector(RuntimeException toThrow) {
      this.toThrow = toThrow;
    }

    public boolean isPausingOrPaused() {
      return false;
    }

    public boolean isPaused() {
      return false;
    }

    public void notifyReadyToGC() {
      return;
    }

    public void requestGCPause() {
      return;
    }

    public void notifyGCComplete() {
      return;
    }

    public void blockUntilReadyToGC() {
      return;
    }

    public Set collect(Filter traverser, Collection roots, Set managedObjectIds) {
      throw toThrow;
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      return out.print(getClass().getName());
    }

    public Set collect(Filter traverser, Collection roots, Set managedObjectIds, LifeCycleState state) {
      return collect(traverser, roots, managedObjectIds);
    }

    public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
      // do nothing

    }

    public void gc() {
      throw toThrow;
    }

    public void addNewReferencesTo(Set rescueIds) {
      // do nothing

    }

    public void start() {
      gcState.start();
    }

    public void stop() {
      // do nothing
    }

    public void setState(StoppableThread st) {
      this.gcState = st;
    }

    public void addListener(ObjectManagerEventListener listener) {
      // do nothing

    }

    public GCStats[] getGarbageCollectorStats() {
      return null;
    }

  }

  private class TestThreadGroup extends ThreadGroup {

    private final LinkedQueue exceptionQueue;

    public TestThreadGroup(LinkedQueue exceptionQueue) {
      super("test thread group");
      this.exceptionQueue = exceptionQueue;
    }

    public void uncaughtException(Thread t, Throwable e) {
      try {
        exceptionQueue.put(e);
      } catch (InterruptedException ie) {
        fail(ie);
      }
    }

  }

  private class GCCaller implements Runnable {

    public void run() {
      objectManager.gc();
    }
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static class TestObjectManagerConfig extends ObjectManagerConfig {

    public long    myGCThreadSleepTime = 100;
    public boolean paranoid;

    public TestObjectManagerConfig() {
      super(10000, true, true, true, 1000);
    }

    TestObjectManagerConfig(long gcThreadSleepTime, boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, 1000);
      throw new RuntimeException("Don't use me.");
    }

    public long gcThreadSleepTime() {
      return myGCThreadSleepTime;
    }

    public boolean paranoid() {
      return paranoid;
    }
  }

  private static class TestMOFaulter extends Thread {

    private final ObjectManagerImpl  objectManager;
    private final ManagedObjectStore store;
    private final TestSink           faultSink;

    public TestMOFaulter(ObjectManagerImpl objectManager, ManagedObjectStore store, TestSink faultSink) {
      this.store = store;
      this.faultSink = faultSink;
      this.objectManager = objectManager;
      setName("TestMOFaulter");
      setDaemon(true);
    }

    public void run() {
      while (true) {
        try {
          ManagedObjectFaultingContext ec = (ManagedObjectFaultingContext) faultSink.take();
          objectManager.addFaultedObject(ec.getId(), store.getObjectByID(ec.getId()), ec.isRemoveOnRelease());
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }

  private static class TestMOFlusher extends Thread {

    private final ObjectManagerImpl objectManager;
    private final TestSink          flushSink;

    public TestMOFlusher(ObjectManagerImpl objectManager, TestSink flushSink) {
      this.objectManager = objectManager;
      this.flushSink = flushSink;
      setName("TestMOFlusher");
      setDaemon(true);
    }

    public void run() {
      while (true) {
        try {
          ManagedObjectFlushingContext ec = (ManagedObjectFlushingContext) flushSink.take();
          objectManager.flushAndEvict(ec.getObjectToFlush());
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }
  }
}
