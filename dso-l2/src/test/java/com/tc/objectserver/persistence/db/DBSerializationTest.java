/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.io.serializer.api.StringIndex;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * XXX: This test needs to test more of the persistor interface.
 */
public class DBSerializationTest extends TCTestCase {

  static {
    ManagedObjectStateFactory.enableLegacyTypes();
  }

  private DBPersistorImpl                persistor;
  private PersistenceTransactionProvider ptp;
  private BerkeleyDBEnvironment          env;
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

  @Override
  public void setUp() throws Exception {
    // XXX: This static temp directory is here to solve file problems
    // on Windows. Each test spawns a new instance of the test class
    // which causes a cleaning of the old temp directory which fails
    // because the je lock file can't be removed because it's still open
    // by sleepycat (sleepycat has a static cache).
    // The static temp directory is here to stop subsequent test instances
    // from trying to clean the temp directory and failing.
    if (tempDirectory == null) {
      tempDirectory = getTempDirectory();
    }
    this.logger = TCLogging.getLogger(getClass());
    this.rootIDs = new HashSet();
    this.rootNames = new HashSet();
    this.mos = new HashSet();
    this.imo = new ObjectInstanceMonitorImpl();

  }

  @Override
  public void tearDown() throws Exception {
    this.persistor = null;
    this.ptp = null;
    this.env = null;
    this.mop = null;
    this.logger = null;
    this.stringIndex = null;
    this.rootIDs = null;
    this.rootNames = null;
    this.mos = null;
  }

  public void testCustomSerializer() throws Exception {
    File dbHome = newDBHome();
    initDB(dbHome);

    final PersistenceTransaction ptx = this.ptp.newTransaction();

    final String rootOne = "rootName";
    final ObjectID rootID = newObjectID();

    // add some roots and objects
    addRoot(ptx, rootOne, rootID);

    for (int i = 0; i < 100; i++) {
      final ObjectID oid = newObjectID();
      addRoot(ptx, "foo" + i, oid);
      final ManagedObject mo = newManagedObject(oid, i);
      assertTrue(mo.isDirty());
      this.mos.add(mo);
    }
    this.mop.saveAllObjects(ptx, this.mos);

    ManagedObject mo = newPhysicalObject(newObjectID());
    this.mop.saveObject(ptx, mo);
    this.mos.add(mo);

    ptx.commit();

    System.err.println("String index before: " + this.stringIndex);
    initDB(dbHome);

    System.err.println("String index after: " + this.stringIndex);

    assertEquals(rootID, this.mop.loadRootID(rootOne));
    assertNotSame(rootID, this.mop.loadRootID(rootOne));
    assertEquals(this.rootIDs, this.mop.loadRoots());
    assertEquals(this.rootNames, this.mop.loadRootNames());

    for (final Iterator i = this.mos.iterator(); i.hasNext();) {
      final ManagedObjectImpl test = (ManagedObjectImpl) i.next();
      final ManagedObjectImpl loaded = (ManagedObjectImpl) this.mop.loadObjectByID(test.getID());
      assertFalse(test.isDirty());
      assertFalse(loaded.isDirty());
      assertFalse(loaded.isNew());
      assertTrue(test.isEqual(loaded));
      assertNotSame(test, loaded);
      assertNotSame(this.mop.loadObjectByID(test.getID()), this.mop.loadObjectByID(test.getID()));

      final byte type = loaded.getManagedObjectState().getType();
      switch (type) {
        case ManagedObjectState.PHYSICAL_TYPE:
          loaded.apply(newPhysicalDNA(true), new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(),
                       this.imo, false);
          break;
        case ManagedObjectState.MAP_TYPE:
        case ManagedObjectState.PARTIAL_MAP_TYPE:
          loaded.apply(newLogicalMapDNA(true), new TransactionID(++this.transactionSequence),
                       new ApplyTransactionInfo(), this.imo, false);
          break;
        case ManagedObjectState.LIST_TYPE:
          loaded.apply(newLogicalListDNA(true), new TransactionID(++this.transactionSequence),
                       new ApplyTransactionInfo(), this.imo, false);
          break;
        case ManagedObjectState.SET_TYPE:
          loaded.apply(newLogicalSetDNA(true), new TransactionID(++this.transactionSequence),
                       new ApplyTransactionInfo(), this.imo, false);
          break;
        case ManagedObjectState.ARRAY_TYPE:
          loaded.apply(newLogicalArrayDNA(true), new TransactionID(++this.transactionSequence),
                       new ApplyTransactionInfo(), this.imo, false);
          break;
        case ManagedObjectState.LITERAL_TYPE:
          loaded.apply(newLiteralDNA(true), new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(),
                       this.imo, false);
          break;
      }

    }
  }

  private ManagedObject newManagedObject(final ObjectID oid, final int i) {
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
        return newLogicalListObject(oid);
      default:
        return newLogicalSetObject(oid);
    }
  }

  private ManagedObject newLogicalSetObject(final ObjectID oid) {
    return newLogicalObject(oid, newLogicalSetDNA(false));
  }

  private TestDNA newLogicalSetDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new Integer(10343) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { "Hello" });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(25) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { newLong() });
    final TestDNA dna = new TestDNA(cursor, HashSet.class.getName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  private ManagedObject newLogicalListObject(final ObjectID oid) {
    return newLogicalObject(oid, newLogicalListDNA(false));
  }

  private TestDNA newLogicalListDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new Integer(10343) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { "Hello" });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(25) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { newLong() });
    final TestDNA dna = new TestDNA(cursor, ArrayList.class.getName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  private ManagedObject newLogicalLiteralObject(final ObjectID oid) {
    return newLogicalObject(oid, newLiteralDNA(false));
  }

  private TestDNA newLiteralDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    final Short s = Short.valueOf((short) 0x0045);
    cursor.addLiteralAction("literal", s);
    final TestDNA dna = new TestDNA(cursor, Short.class.getName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  private ManagedObject newLogicalArrayObject(final ObjectID oid) {
    return newLogicalObject(oid, newLogicalArrayDNA(false));
  }

  private TestDNA newLogicalArrayDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    final Object[] array = new Object[] { newLong(), newLong(), newLong() };
    cursor.addArrayAction(array);
    final TestDNA dna = new TestDNA(cursor, array.getClass().getName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  Random r = new Random();

  private Long newLong() {
    return Long.valueOf(this.r.nextLong());
  }

  private ManagedObject newLogicalObject(final ObjectID objectID, final TestDNA dna) {
    final ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(), this.imo, false);
    return rv;
  }

  private ManagedObject newLogicalMapObject(final ObjectID oid) {
    return newLogicalObject(oid, newLogicalMapDNA(false));
  }

  private void addRoot(final PersistenceTransaction ptx, final String rootName, final ObjectID objectID) {
    this.mop.addRoot(ptx, rootName, objectID);
    this.rootIDs.add(objectID);
    this.rootNames.add(rootName);
  }

  private ObjectID newObjectID() {
    return new ObjectID(++this.objectIDSequence);
  }

  private ManagedObject newPhysicalObject(final ObjectID objectID) {
    final ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    final TestDNA dna = newPhysicalDNA(false);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(), this.imo, false);
    return rv;
  }

  private TestDNA newPhysicalDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("stringField", "Foo", true);
    cursor.addPhysicalAction("referenceField", newObjectID(), true);
    final TestDNA dna = new TestDNA(cursor);
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  private TestDNA newLogicalMapDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(10), "King Kong" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(20), "Mad Max" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(25), "Mummy Returns" });
    final TestDNA dna = new TestDNA(cursor, HashMap.class.getName());
    dna.version = this.version++;
    dna.isDelta = delta;
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

  private void initDB(File dbHome) throws IOException, TCDatabaseException {
    if (env != null) env.close();
    env = new BerkeleyDBEnvironment(true, dbHome);
    SerializationAdapterFactory saf = null;
    saf = new CustomSerializationAdapterFactory();
    persistor = new DBPersistorImpl(logger, env, saf);
    ptp = persistor.getPersistenceTransactionProvider();
    mop = persistor.getManagedObjectPersistor();
    stringIndex = persistor.getStringIndex();

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), this.persistor);
  }
}
