/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.tc.io.serializer.api.StringIndex;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * XXX: This test needs to test more of the persistor interface.
 */
public class SleepycatSerializationTest extends TCTestCase {
  private SleepycatPersistor             persistor;
  private PersistenceTransactionProvider ptp;
  private DBEnvironment                  env;
  private ManagedObjectPersistor         mop;
  private TCLogger                       logger;
  private StringIndex                    stringIndex;
  private int                            transactionSequence;
  private int                            objectIDSequence;
  private Set                            rootIDs;
  private Set                            rootNames;
  private Set                            mos;
  private ObjectInstanceMonitor          imo;
  private int                            version       = 0;
  private static int                     dbHomeCounter = 0;
  private static File                    tempDirectory;

  public void setUp() throws Exception {
    // XXX: This static temp directory is here to solve file problems
    // on Windows. Each test spawns a new instance of the test class
    // which causes a cleaning of the old temp directory which fails
    // because the je lock file can't be removed because it's still open
    // by sleepycat (sleepycat has a static cache).
    // The static temp directory is here to stop subsequent test instances
    // from trying to clean the temp directory and failing.
    if (tempDirectory == null) tempDirectory = getTempDirectory();
    logger = TCLogging.getLogger(getClass());
    rootIDs = new HashSet();
    rootNames = new HashSet();
    mos = new HashSet();
    imo = new ObjectInstanceMonitorImpl();

  }

  public void tearDown() throws Exception {
    persistor = null;
    ptp = null;
    env = null;
    mop = null;
    logger = null;
    stringIndex = null;
    rootIDs = null;
    rootNames = null;
    mos = null;
  }

  public void testSleepycatSerializer() throws Exception {
    SerializationAdapterFactory saf = new SleepycatSerializationAdapterFactory();
    doTest(saf);
  }

  public void testCustomSerializer() throws Exception {
    SerializationAdapterFactory saf = new CustomSerializationAdapterFactory();
    doTest(saf);
  }

  public void doTest(SerializationAdapterFactory saf) throws Exception {
    File dbHome = newDBHome();
    initDB(dbHome, saf);

    PersistenceTransaction ptx = ptp.newTransaction();

    String rootOne = "rootName";
    ObjectID rootID = newObjectID();

    // add some roots and objects
    addRoot(ptx, rootOne, rootID);

    for (int i = 0; i < 100; i++) {
      ObjectID oid = newObjectID();
      addRoot(ptx, "foo" + i, oid);
      ManagedObject mo = newManagedObject(oid, i);
      assertTrue(mo.isDirty());
      mos.add(mo);
    }
    mop.saveAllObjects(ptx, mos);

    ManagedObject mo = newPhysicalObject(newObjectID());
    mop.saveObject(ptx, mo);
    mos.add(mo);

    mo = newLogicalDateObject(newObjectID());
    mop.saveObject(ptx, mo);
    mos.add(mo);

    ptx.commit();

    System.err.println("String index before: " + stringIndex);
    initDB(dbHome, saf);

    System.err.println("String index after: " + stringIndex);

    assertEquals(rootID, mop.loadRootID(rootOne));
    assertNotSame(rootID, mop.loadRootID(rootOne));
    assertEquals(rootIDs, mop.loadRoots());
    assertEquals(rootNames, mop.loadRootNames());

    for (Iterator i = mos.iterator(); i.hasNext();) {
      ManagedObjectImpl test = (ManagedObjectImpl) i.next();
      ManagedObjectImpl loaded = (ManagedObjectImpl) mop.loadObjectByID(test.getID());
      assertFalse(test.isDirty());
      assertFalse(loaded.isDirty());
      assertFalse(test.isNew());
      assertFalse(loaded.isNew());
      assertTrue(test.isEqual(loaded));
      assertNotSame(test, loaded);
      assertNotSame(mop.loadObjectByID(test.getID()), mop.loadObjectByID(test.getID()));

      byte type = loaded.getManagedObjectState().getType();
      switch (type) {
        case ManagedObjectState.PHYSICAL_TYPE:
          loaded.apply(newPhysicalDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.MAP_TYPE:
        case ManagedObjectState.PARTIAL_MAP_TYPE:
          loaded.apply(newLogicalMapDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.LIST_TYPE:
          loaded.apply(newLogicalListDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.SET_TYPE:
          loaded.apply(newLogicalSetDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.ARRAY_TYPE:
          loaded.apply(newLogicalArrayDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.LINKED_HASHMAP_TYPE:
          loaded.apply(newLogicalLinkedHashMapDNA(), new TransactionID(++transactionSequence), new BackReferences(),
                       imo, false);
          break;
        case ManagedObjectState.DATE_TYPE:
          loaded.apply(newLogicalDateDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.LITERAL_TYPE:
          loaded.apply(newLiteralDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.TREE_MAP_TYPE:
          loaded.apply(newLogicalTreeMapDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
        case ManagedObjectState.TREE_SET_TYPE:
          loaded.apply(newLogicalTreeSetDNA(), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
          break;
      }

    }
  }

  private ManagedObject newManagedObject(ObjectID oid, int i) {
    switch (i % 10) {
      case 0:
        return newPhysicalObject(oid);
      case 1:
        return newLogicalMapObject(oid);
      case 2:
        return newLogicalArrayObject(oid);
      case 3:
        return newLogicalLiteralObject(oid);
      case 4:
        return newLogicalLinkedHashMapObject(oid);
      case 5:
        return newLogicalListObject(oid);
      case 6:
        return newLogicalSetObject(oid);
      case 7:
        return newLogicalTreeSetObject(oid);
      case 8:
        return newLogicalTreeMapObject(oid);
      default:
        return newLogicalDateObject(oid);
    }
  }

  private ManagedObject newLogicalTreeMapObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalTreeMapDNA());
  }

  private TestDNA newLogicalTreeMapDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Short((short) 10), "good bad and ugly" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Boolean(true), "mapped" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Boolean(true), "Remapped" });
    TestDNA dna = new TestDNA(cursor, TreeMap.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalTreeSetObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalTreeSetDNA());
  }

  private TestDNA newLogicalTreeSetDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new Integer(10343) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { "Hello" });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(25) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { newLong() });
    TestDNA dna = new TestDNA(cursor, TreeSet.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalSetObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalSetDNA());
  }

  private TestDNA newLogicalSetDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new Integer(10343) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { "Hello" });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(25) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { newLong() });
    TestDNA dna = new TestDNA(cursor, HashSet.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalListObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalListDNA());
  }

  private TestDNA newLogicalListDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new Integer(10343) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { "Hello" });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(25) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { newLong() });
    TestDNA dna = new TestDNA(cursor, ArrayList.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalLinkedHashMapObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalLinkedHashMapDNA());
  }

  private TestDNA newLogicalLinkedHashMapDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("java.util.LinkedHashMap.accessOrder", Boolean.TRUE);
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(10), "King Kong" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(20), "Mad Max" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(25), "Mummy Returns" });
    cursor.addLogicalAction(SerializationUtil.GET, new Object[] { new Integer(20) });
    TestDNA dna = new TestDNA(cursor, LinkedHashMap.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalLiteralObject(ObjectID oid) {
    return newLogicalObject(oid, newLiteralDNA());
  }

  private TestDNA newLiteralDNA() {
    TestDNACursor cursor = new TestDNACursor();
    Short s = new Short((short) 0x0045);
    cursor.addLiteralAction("literal", s);
    TestDNA dna = new TestDNA(cursor, Short.class.getName());
    dna.version = version++;
    return dna;
  }

  private ManagedObject newLogicalArrayObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalArrayDNA());
  }

  private TestDNA newLogicalArrayDNA() {
    TestDNACursor cursor = new TestDNACursor();
    Object[] array = new Object[] { newLong(), newLong(), newLong() };
    cursor.addArrayAction(array);
    TestDNA dna = new TestDNA(cursor, array.getClass().getName());
    dna.version = version++;
    return dna;
  }

  Random r = new Random();

  private Long newLong() {
    return new Long(r.nextLong());
  }

  private ManagedObject newLogicalDateObject(ObjectID objectID) {
    return newLogicalObject(objectID, newLogicalDateDNA());
  }

  private ManagedObject newLogicalObject(ObjectID objectID, TestDNA dna) {
    ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++transactionSequence), new BackReferences(), imo, false);
    assertFalse(rv.isNew());
    return rv;
  }

  private ManagedObject newLogicalMapObject(ObjectID oid) {
    return newLogicalObject(oid, newLogicalMapDNA());
  }

  private void addRoot(PersistenceTransaction ptx, String rootName, ObjectID objectID) {
    mop.addRoot(ptx, rootName, objectID);
    rootIDs.add(objectID);
    rootNames.add(rootName);
  }

  private ObjectID newObjectID() {
    return new ObjectID(++objectIDSequence);
  }

  private ManagedObject newPhysicalObject(ObjectID objectID) {
    ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    TestDNA dna = newPhysicalDNA();
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++transactionSequence), new BackReferences(), imo, false);
    assertFalse(rv.isNew());
    return rv;
  }

  private TestDNA newPhysicalDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("stringField", "Foo");
    cursor.addPhysicalAction("referenceField", newObjectID());
    TestDNA dna = new TestDNA(cursor);
    dna.version = version++;
    return dna;
  }

  private TestDNA newLogicalMapDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(10), "King Kong" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(20), "Mad Max" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new Integer(25), "Mummy Returns" });
    TestDNA dna = new TestDNA(cursor, HashMap.class.getName());
    dna.version = version++;
    return dna;
  }

  private TestDNA newLogicalDateDNA() {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.SET_TIME, new Object[] { new Long(100233434L) });
    TestDNA dna = new TestDNA(cursor, Date.class.getName());
    dna.version = version++;
    return dna;
  }

  private File newDBHome() {
    File file;
    // XXX: UGH... this extra increment is here because of a weird interaction with the static cache of the sleepycat
    // database and JUnit's test running junk, and something else I don't quite understand about the File.exists(). I
    // had to add this to ensure that the db counter was actually incremented and that a new directory was actually
    // used.
    ++dbHomeCounter;
    for (file = new File(tempDirectory, "db" + dbHomeCounter); file.exists(); ++dbHomeCounter) {
      //
    }
    assertFalse(file.exists());
    return file;
  }

  private void initDB(File dbHome, SerializationAdapterFactory saf) throws IOException, DatabaseException {
    if (env != null) env.close();
    env = new DBEnvironment(true, dbHome);
    persistor = new SleepycatPersistor(logger, env, saf);
    ptp = persistor.getPersistenceTransactionProvider();
    mop = persistor.getManagedObjectPersistor();
    stringIndex = persistor.getStringIndex();

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);

  }
}
