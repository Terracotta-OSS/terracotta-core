/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractDBUtilsTestBase extends TCTestCase {

  private static final TCLogger   logger  = TCLogging.getLogger(AbstractDBUtilsTestBase.class);
  private int                     transactionSequence;
  private int                     objectIDSequence;
  private Set                     rootIDs;
  private Set                     rootNames;
  private Set                     mos;
  private ObjectInstanceMonitor   imo;
  private int                     version = 1;
  private int                     dnaRequestCount;
  protected BerkeleyDBEnvironment dbenv;

  protected static class SampleDNA1 extends AbstractSampleDNA1 {

    public SampleDNA1(final DNACursor cursor, final long version, final boolean isDelta) {
      super("Sample1", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA2 extends AbstractSampleDNA1 {

    public SampleDNA2(final DNACursor cursor, final long version, final boolean isDelta) {
      super("Sample2", cursor, version, isDelta);
    }
  }

  @Override
  public void setUp() throws Exception {
    reset();
  }

  @Override
  public void tearDown() throws Exception {

    this.rootIDs = null;
    this.rootNames = null;
    this.mos = null;
  }

  protected void reset() {
    this.rootIDs = new HashSet();
    this.rootNames = new HashSet();
    this.mos = new HashSet();
    this.imo = new ObjectInstanceMonitorImpl();
    this.transactionSequence = 0;
    this.objectIDSequence = 0;
    this.version = 1;
    this.dnaRequestCount = 0;

  }

  protected void populateSleepycatDB(final DBPersistorImpl sleepycatPersistor) {
    final PersistenceTransactionProvider persistenceTransactionProvider = sleepycatPersistor
        .getPersistenceTransactionProvider();
    final ManagedObjectPersistor mop = sleepycatPersistor.getManagedObjectPersistor();
    final PersistenceTransaction ptx = persistenceTransactionProvider.newTransaction();

    final String rootOne = "rootName";
    final ObjectID rootID = newObjectID();

    // add some roots and objects
    addRoot(mop, ptx, rootOne, rootID);

    for (int i = 0; i < 100; i++) {
      final ObjectID oid = newObjectID();
      // addRoot(mop, ptx, "foo" + i, oid);
      final ManagedObject mo = newManagedObject(oid, i);
      assertTrue(mo.isDirty());
      this.mos.add(mo);
      mop.addNewObject(mo);
    }
    mop.saveAllObjects(ptx, this.mos);

    final ManagedObject mo = newPhysicalObject(newObjectID());
    mop.saveObject(ptx, mo);
    this.mos.add(mo);
    mop.addNewObject(mo);

    ptx.commit();

    assertEquals(rootID, mop.loadRootID(rootOne));
    assertNotSame(rootID, mop.loadRootID(rootOne));
    assertEquals(this.rootIDs, mop.loadRoots());
    assertEquals(this.rootNames, mop.loadRootNames());

    for (final Iterator i = this.mos.iterator(); i.hasNext();) {
      final ManagedObjectImpl test = (ManagedObjectImpl) i.next();
      final ManagedObjectImpl loaded = (ManagedObjectImpl) mop.loadObjectByID(test.getID());
      assertFalse(test.isDirty());
      assertFalse(loaded.isDirty());
      assertFalse(loaded.isNew());
      assertTrue(test.isEqual(loaded));
      assertNotSame(test, loaded);
      assertNotSame(mop.loadObjectByID(test.getID()), mop.loadObjectByID(test.getID()));
      loaded.apply(newPhysicalDNA(true), new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(),
                   this.imo, false);
    }
  }

  private ManagedObject newManagedObject(final ObjectID oid, final int i) {
    return newPhysicalObject(oid);

  }

  private void addRoot(final ManagedObjectPersistor mop, final PersistenceTransaction ptx, final String rootName,
                       final ObjectID objectID) {
    mop.addRoot(ptx, rootName, objectID);
    this.rootIDs.add(objectID);
    this.rootNames.add(rootName);
  }

  private ObjectID newObjectID() {
    return new ObjectID(++this.objectIDSequence);
  }

  private ManagedObject newPhysicalObject(final ObjectID objectID) {
    final ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    final DNA dna = newPhysicalDNA(false);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(), this.imo, false);
    return rv;
  }

  private DNA newPhysicalDNA(final boolean delta) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("stringField", "Foo", true);
    cursor.addPhysicalAction("referenceField", newObjectID(), true);
    this.dnaRequestCount++;

    DNA dna = null;
    if (this.dnaRequestCount == 1) {
      cursor.reset();
      dna = new SampleDNA1(cursor, this.version++, delta);
    }
    if (this.dnaRequestCount == 2) {
      cursor.reset();
      dna = new SampleDNA2(cursor, this.version++, delta);
    }
    if (this.dnaRequestCount == 3) {
      cursor.reset();
      dna = new SampleDNA3(cursor, this.version++, delta);
    }
    if (this.dnaRequestCount == 4) {
      cursor.reset();
      dna = new SampleDNA4(cursor, this.version++, delta);
    }
    if (this.dnaRequestCount == 5) {
      cursor.reset();
      dna = new SampleDNA5(cursor, this.version++, delta);
      this.dnaRequestCount = 0;
    }

    return dna;
  }

  protected DBPersistorImpl getSleepycatPersistor(final File dir) {
    DBPersistorImpl persistor = null;
    try {
      this.dbenv = new BerkeleyDBEnvironment(true, dir);
      final SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
      final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
      persistor = new DBPersistorImpl(logger, this.dbenv, serializationAdapterFactory);
      ManagedObjectStateFactory.disableSingleton(true);
      ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
    return persistor;
  }

  protected static class SampleDNA3 extends AbstractSampleDNA1 {

    public SampleDNA3(final DNACursor cursor, final long version, final boolean isDelta) {
      super("Sample3", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA4 extends AbstractSampleDNA1 {

    public SampleDNA4(final DNACursor cursor, final long version, final boolean isDelta) {
      super("Sample4", cursor, version, isDelta);
    }
  }

  protected static class SampleDNA5 extends AbstractSampleDNA1 {

    public SampleDNA5(final DNACursor cursor, final long version, final boolean isDelta) {
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

    public AbstractSampleDNA1(final String typeName, final DNACursor cursor, final long version, final boolean isDelta) {
      this.typeName = typeName;
      this.cursor = cursor;
      this.version = version;
      this.isDelta = isDelta;
    }

    public String getTypeName() {
      return this.typeName;
    }

    public ObjectID getObjectID() throws DNAException {
      return this.objectID;
    }

    public DNACursor getCursor() {
      return this.cursor;
    }

    public boolean hasLength() {
      return false;
    }

    public int getArraySize() {
      return 0;
    }

    public String getDefiningLoaderDescription() {
      return this.loaderDesc;
    }

    public ObjectID getParentObjectID() throws DNAException {
      return this.parentObjectID;
    }

    public void addPhysicalAction(final String field, final Object value) throws DNAException {
      return;
    }

    public void addLogicalAction(final int method, final Object[] parameters) {
      return;
    }

    public long getVersion() {
      return this.version;
    }

    public boolean isDelta() {
      return this.isDelta;
    }

    @Override
    public String toString() {
      return "TestDNA(" + this.objectID + ", version = " + this.version + ")";
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

  public AbstractDBUtilsTestBase(final String arg0) {
    super(arg0);
  }

}