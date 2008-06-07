/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractDBUtilsTestBase extends TCTestCase {

  private static final TCLogger logger  = TCLogging.getLogger(FastLoadOidLogAnalysisTest.class);
  private int                   transactionSequence;
  private int                   objectIDSequence;
  private Set                   rootIDs;
  private Set                   rootNames;
  private Set                   mos;
  private ObjectInstanceMonitor imo;
  private int                   version = 1;
  private int                   dnaRequestCount;

  protected static class SampleDNA1 extends AbstractSampleDNA1 {

    public SampleDNA1(DNACursor cursor, long version, boolean isDelta) {
      super("Sample1", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA2 extends AbstractSampleDNA1 {

    public SampleDNA2(DNACursor cursor, long version, boolean isDelta) {
      super("Sample2", cursor, version, isDelta);
    }
  }

  public void setUp() throws Exception {
    reset();
  }

  public void tearDown() throws Exception {

    rootIDs = null;
    rootNames = null;
    mos = null;
  }

  protected void reset() {
    rootIDs = new HashSet();
    rootNames = new HashSet();
    mos = new HashSet();
    imo = new ObjectInstanceMonitorImpl();
    transactionSequence = 0;
    objectIDSequence = 0;
    version = 1;
    dnaRequestCount = 0;


  }

  protected void populateSleepycatDB(SleepycatPersistor sleepycatPersistor) {
    PersistenceTransactionProvider persistenceTransactionProvider = sleepycatPersistor
        .getPersistenceTransactionProvider();
    ManagedObjectPersistor mop = sleepycatPersistor.getManagedObjectPersistor();
    PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();

    String rootOne = "rootName";
    ObjectID rootID = newObjectID();

    // add some roots and objects
    addRoot(mop, ptx, rootOne, rootID);

    for (int i = 0; i < 100; i++) {
      ObjectID oid = newObjectID();
      // addRoot(mop, ptx, "foo" + i, oid);
      ManagedObject mo = newManagedObject(oid, i);
      assertTrue(mo.isDirty());
      mos.add(mo);
    }
    mop.saveAllObjects(ptx, mos);

    ManagedObject mo = newPhysicalObject(newObjectID());
    mop.saveObject(ptx, mo);
    mos.add(mo);

    ptx.commit();

    assertEquals(rootID, mop.loadRootID(rootOne));
    assertNotSame(rootID, mop.loadRootID(rootOne));
    assertEquals(rootIDs, mop.loadRoots());
    assertEquals(rootNames, mop.loadRootNames());

    for (Iterator i = mos.iterator(); i.hasNext();) {
      ManagedObjectImpl test = (ManagedObjectImpl) i.next();
      ManagedObjectImpl loaded = (ManagedObjectImpl) mop.loadObjectByID(test.getID());
      assertFalse(test.isDirty());
      assertFalse(loaded.isDirty());
      assertFalse(loaded.isNew());
      assertTrue(test.isEqual(loaded));
      assertNotSame(test, loaded);
      assertNotSame(mop.loadObjectByID(test.getID()), mop.loadObjectByID(test.getID()));
      loaded.apply(newPhysicalDNA(true), new TransactionID(++transactionSequence), new BackReferences(), imo, false);
    }
  }

  private ManagedObject newManagedObject(ObjectID oid, int i) {
    return newPhysicalObject(oid);

  }

  private void addRoot(ManagedObjectPersistor mop, PersistenceTransaction ptx, String rootName, ObjectID objectID) {
    mop.addRoot(ptx, rootName, objectID);
    rootIDs.add(objectID);
    rootNames.add(rootName);
  }

  private ObjectID newObjectID() {
    return new ObjectID(++objectIDSequence);
  }

  private ManagedObject newPhysicalObject(ObjectID objectID) {
    ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    DNA dna = newPhysicalDNA(false);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++transactionSequence), new BackReferences(), imo, false);
    return rv;
  }

  private DNA newPhysicalDNA(boolean delta) {
    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("stringField", "Foo");
    cursor.addPhysicalAction("referenceField", newObjectID());
    dnaRequestCount++;

    DNA dna = null;
    if (dnaRequestCount == 1) {
      cursor.reset();
      dna = new SampleDNA1(cursor, version++, delta);
    }
    if (dnaRequestCount == 2) {
      cursor.reset();
      dna = new SampleDNA2(cursor, version++, delta);
    }
    if (dnaRequestCount == 3) {
      cursor.reset();
      dna = new SampleDNA3(cursor, version++, delta);
    }
    if (dnaRequestCount == 4) {
      cursor.reset();
      dna = new SampleDNA4(cursor, version++, delta);
    }
    if (dnaRequestCount == 5) {
      cursor.reset();
      dna = new SampleDNA5(cursor, version++, delta);
      dnaRequestCount = 0;
    }

    return dna;
  }

  protected SleepycatPersistor getSleepycatPersistor(File dir) {
    DBEnvironment env;
    SleepycatPersistor persistor = null;
    try {
      env = new DBEnvironment(true, dir);
      SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
      final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
      persistor = new SleepycatPersistor(logger, env, serializationAdapterFactory);
      ManagedObjectStateFactory.disableSingleton(true);
      ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    return persistor;
  }

  protected static class SampleDNA3 extends AbstractSampleDNA1 {

    public SampleDNA3(DNACursor cursor, long version, boolean isDelta) {
      super("Sample3", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA4 extends AbstractSampleDNA1 {

    public SampleDNA4(DNACursor cursor, long version, boolean isDelta) {
      super("Sample4", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA5 extends AbstractSampleDNA1 {

    public SampleDNA5(DNACursor cursor, long version, boolean isDelta) {
      super("Sample5", cursor, version, isDelta);
    }
  }

  protected static abstract class AbstractSampleDNA1 implements DNA {
    public DNACursor cursor;
    public ObjectID  objectID;
    public long      version;
    public String    typeName;
    public ObjectID  parentObjectID = ObjectID.NULL_ID;
    public boolean   isDelta;
    public String    loaderDesc     = "system.loader";

    public AbstractSampleDNA1(String typeName, DNACursor cursor, long version, boolean isDelta) {
      this.typeName = typeName;
      this.cursor = cursor;
      this.version = version;
      this.isDelta = isDelta;
    }

    public String getTypeName() {
      return typeName;
    }

    public ObjectID getObjectID() throws DNAException {
      return objectID;
    }

    public DNACursor getCursor() {
      return cursor;
    }

    public boolean hasLength() {
      return false;
    }

    public int getArraySize() {
      return 0;
    }

    public String getDefiningLoaderDescription() {
      return loaderDesc;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return parentObjectID;
    }

    public void setHeaderInformation(ObjectID id, ObjectID parentID, String type, int length, long version)
        throws DNAException {
      return;
    }

    public void addPhysicalAction(String field, Object value) throws DNAException {
      return;
    }

    public void addLogicalAction(int method, Object[] parameters) {
      return;
    }

    public long getVersion() {
      return this.version;
    }

    public boolean isDelta() {
      return isDelta;
    }

    public String toString() {
      return "TestDNA(" + objectID + ", version = " + version + ")";
    }
  }

  protected static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    public ManagedObjectChangeListener getListener() {
      return new NullManagedObjectChangeListener();

    }
  }

  public AbstractDBUtilsTestBase() {
    super();
  }

  public AbstractDBUtilsTestBase(String arg0) {
    super(arg0);
  }

}