/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.db.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.db.DBPersistorImpl;
import com.tc.objectserver.persistence.db.HashMapBackingMapFactory;
import com.tc.objectserver.persistence.db.ManagedObjectPersistorImpl;
import com.tc.objectserver.persistence.db.PersistableCollectionFactory;
import com.tc.objectserver.persistence.db.TCCollectionsPersistor;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;
import com.tc.objectserver.persistence.impl.TestMutableSequence;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.DBFactory;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadDumpUtil;

import java.io.File;

public class ManagedObjectStateSerializationTestBase extends TCTestCase {
  private final TCLogger                 logger   = TCLogging.getTestingLogger(getClass());
  private ObjectID                       objectID = new ObjectID(2000);

  private DBPersistorImpl                persistor;
  private DBEnvironment                  env;
  private ManagedObjectPersistorImpl     managedObjectPersistor;
  private PersistenceTransactionProvider ptp;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.env = newDBEnvironment();
    final CustomSerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
    this.persistor = new DBPersistorImpl(this.logger, this.env, serializationAdapterFactory);

    this.ptp = this.env.getPersistenceTransactionProvider();
    final PersistableCollectionFactory collectionFactory = new PersistableCollectionFactory(
                                                                                            new HashMapBackingMapFactory(),
                                                                                            this.env.isParanoidMode());
    final TCCollectionsPersistor collectionsPersistor = new TCCollectionsPersistor(this.logger,
                                                                                   this.env.getMapsDatabase(),
                                                                                   collectionFactory,
                                                                                   new TCCollectionsSerializerImpl());
    this.managedObjectPersistor = new ManagedObjectPersistorImpl(this.logger, serializationAdapterFactory, this.env,
                                                                 new TestMutableSequence(), this.env.getRootDatabase(),
                                                                 this.ptp, collectionsPersistor,
                                                                 this.env.isParanoidMode(), new ObjectStatsRecorder());
    final NullManagedObjectChangeListenerProvider listenerProvider = new NullManagedObjectChangeListenerProvider();
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(listenerProvider, persistor);

    // wait for completion of daemon threads launched by getAllObjectIDs() & getAllMapsObjectIDs()
    this.managedObjectPersistor.snapshotObjectIDs();
    this.managedObjectPersistor.snapshotEvictableObjectIDs();
    this.managedObjectPersistor.snapshotMapTypeObjectIDs();
  }

  private DBEnvironment newDBEnvironment() throws Exception {
    final File dbHome = new File(getTempDirectory(), getClass().getName() + "db");
    dbHome.mkdirs();
    return DBFactory.getInstance().createEnvironment(true, dbHome);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      this.managedObjectPersistor.close();
      this.persistor.close();
    } catch (Exception e) {
      System.err.println("Closing failed, printing stack trace...");
      System.err.println(ThreadDumpUtil.getThreadDump());
    }
    ManagedObjectStateFactory.disableSingleton(false);
  }

  protected ManagedObjectState applyValidation(final String className, final DNACursor dnaCursor) throws Exception {
    this.objectID = new ObjectID(this.objectID.toLong() + 1);
    final ManagedObject mo = new ManagedObjectImpl(this.objectID);

    final TestDNA dna = new TestDNA(dnaCursor);
    dna.typeName = className;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), new NullObjectInstanceMonitor(), false);

    final PersistenceTransaction txn = this.ptp.newTransaction();
    this.managedObjectPersistor.saveObject(txn, mo);
    txn.commit();

    final ManagedObjectState state = mo.getManagedObjectState();
    final TestDNAWriter dnaWriter = dehydrate(state);
    validate(dnaCursor, dnaWriter);

    return state;
  }

  protected void serializationValidation(final ManagedObjectState state, final DNACursor dnaCursor, final byte type)
      throws Exception {
    final ManagedObject loaded = this.managedObjectPersistor.loadObjectByID(this.objectID);
    final TestDNAWriter dnaWriter = dehydrate(loaded.getManagedObjectState());
    validate(dnaCursor, dnaWriter);
  }

  private TestDNAWriter dehydrate(final ManagedObjectState state) throws Exception {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L2_SYNC);
    return dnaWriter;
  }

  private void validate(final DNACursor dnaCursor, final TestDNAWriter writer) throws Exception {
    Assert.assertEquals(dnaCursor.getActionCount(), writer.getActionCount());
    dnaCursor.reset();
    while (dnaCursor.next()) {
      final Object action = dnaCursor.getAction();
      Assert.assertTrue(writer.containsAction(action));
    }
  }
}
