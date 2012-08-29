/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.test.TCTestCase;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
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
  protected BerkeleyDBEnvironment dbenv;

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

    // add another one
    final ObjectID oid = newObjectID();
    final ManagedObject mo = newLogicalObject(oid, newLogicalMapDNA(false, true, 101));
    assertTrue(mo.isDirty());
    this.mos.add(mo);
    mop.addNewObject(mo);
    mop.saveObject(ptx, mo);

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
      // loaded.apply(newPhysicalDNA(true), new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(),
      // this.imo, false);
    }
  }

  private ManagedObject newManagedObject(final ObjectID oid, final int i) {
    switch (i % 10) {
      case 0:
        return newLogicalMapObject(oid);
      case 1:
        return newLogicalArrayObject(oid);
      case 2:
        return newLogicalLiteralObject(oid);
      default:
        return newLogicalListObject(oid);
    }
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
    final TestDNA dna = new TestDNA(cursor, ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName());
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
    final TestDNA dna = new TestDNA(cursor, ManagedObjectStateStaticConfig.TOOLKIT_OBJECT_STRIPE.getClientClassName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
  }

  Random r = new Random();

  private Long newLong() {
    return Long.valueOf(this.r.nextLong());
  }

  private ManagedObject newLogicalMapObject(final ObjectID oid) {
    return newLogicalObject(oid, newLogicalMapDNA(false, false, -1));
  }

  private ManagedObject newLogicalObject(final ObjectID objectID, final TestDNA dna) {
    final ManagedObjectImpl rv = new ManagedObjectImpl(objectID);
    assertTrue(rv.isNew());
    rv.apply(dna, new TransactionID(++this.transactionSequence), new ApplyTransactionInfo(), this.imo, false);
    return rv;
  }

  private TestDNA newLogicalMapDNA(final boolean delta, boolean addOids, int numberOfOids) {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(10), "King Kong" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(20), "Mad Max" });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { Integer.valueOf(25), "Mummy Returns" });

    if (addOids) {
      for (int i = 0; i < numberOfOids; i++) {
        cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { "notExists" + i, newObjectID() });
      }
    }
    final TestDNA dna = new TestDNA(cursor, ManagedObjectStateStaticConfig.TOOLKIT_TYPE_ROOT.getClientClassName());
    dna.version = this.version++;
    dna.isDelta = delta;
    return dna;
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

  protected static abstract class AbstractSampleDNA1 implements DNA {
    public DNACursor cursor;
    public ObjectID  objectID;
    public long      version;
    public String    typeName;
    public ObjectID  parentObjectID = ObjectID.NULL_ID;
    public boolean   isDelta;

    public AbstractSampleDNA1(final String typeName, final DNACursor cursor, final long version, final boolean isDelta) {
      this.typeName = typeName;
      this.cursor = cursor;
      this.version = version;
      this.isDelta = isDelta;
    }

    @Override
    public String getTypeName() {
      return this.typeName;
    }

    @Override
    public ObjectID getObjectID() throws DNAException {
      return this.objectID;
    }

    @Override
    public DNACursor getCursor() {
      return this.cursor;
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
    public ObjectID getParentObjectID() throws DNAException {
      return this.parentObjectID;
    }

    public void addPhysicalAction(final String field, final Object value) throws DNAException {
      return;
    }

    public void addLogicalAction(final int method, final Object[] parameters) {
      return;
    }

    @Override
    public long getVersion() {
      return this.version;
    }

    @Override
    public boolean isDelta() {
      return this.isDelta;
    }

    @Override
    public String toString() {
      return "TestDNA(" + this.objectID + ", version = " + this.version + ")";
    }
  }

  protected static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    @Override
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
