/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.io.serializer.api.StringIndex;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.impl.StringIndexImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.io.File;
import java.io.IOException;

public class SleepycatPersistor implements Persistor {
  private static final int                     DEFAULT_CAPACITY = 50000;

  private final StringIndexPersistor           stringIndexPersistor;
  private final StringIndex                    stringIndex;
  private final ManagedObjectPersistorImpl     managedObjectPersistor;
  private final ClientStatePersistor           clientStatePersistor;
  private final TransactionPersistor           transactionPerisistor;
  private final MutableSequence                globalTransactionIDSequence;
  private final ClassPersistor                 classPersistor;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final DBEnvironment                  env;
  private final SleepycatCollectionFactory     sleepycatCollectionFactory;
  private final PersistentMapStore             clusterStateStore;

  private SleepycatCollectionsPersistor        sleepycatCollectionsPersistor;

  // only for tests
  public SleepycatPersistor(TCLogger logger, DBEnvironment env, SerializationAdapterFactory serializationAdapterFactory)
      throws TCDatabaseException {
    this(logger, env, serializationAdapterFactory, null);
  }

  public SleepycatPersistor(TCLogger logger, DBEnvironment env,
                            SerializationAdapterFactory serializationAdapterFactory, File l2DataPath)
      throws TCDatabaseException {

    open(env, logger);
    this.env = env;

    sanityCheckAndClean(env, l2DataPath, logger);

    CursorConfig rootDBCursorConfig = new CursorConfig();
    rootDBCursorConfig.setReadCommitted(true);
    CursorConfig stringIndexCursorConfig = new CursorConfig();
    stringIndexCursorConfig.setReadCommitted(true);
    this.persistenceTransactionProvider = new SleepycatPersistenceTransactionProvider(env.getEnvironment());
    this.stringIndexPersistor = new SleepycatStringIndexPersistor(persistenceTransactionProvider, env
        .getStringIndexDatabase(), stringIndexCursorConfig, env.getClassCatalogWrapper().getClassCatalog());
    this.stringIndex = new StringIndexImpl(this.stringIndexPersistor, DEFAULT_CAPACITY);
    this.sleepycatCollectionFactory = new SleepycatCollectionFactory();
    this.sleepycatCollectionsPersistor = new SleepycatCollectionsPersistor(logger, env.getMapsDatabase(),
                                                                           sleepycatCollectionFactory);
    this.managedObjectPersistor = new ManagedObjectPersistorImpl(
                                                                 logger,
                                                                 env.getClassCatalogWrapper().getClassCatalog(),
                                                                 serializationAdapterFactory,
                                                                 env,
                                                                 new SleepycatSequence(
                                                                                       this.persistenceTransactionProvider,
                                                                                       logger, 1, 1000, env
                                                                                           .getObjectIDDB()), env
                                                                     .getRootDatabase(), rootDBCursorConfig,
                                                                 this.persistenceTransactionProvider,
                                                                 this.sleepycatCollectionsPersistor, env
                                                                     .isParanoidMode());
    this.clientStatePersistor = new ClientStatePersistorImpl(logger, this.persistenceTransactionProvider,
                                                             new SleepycatSequence(this.persistenceTransactionProvider,
                                                                                   logger, 1, 0, env
                                                                                       .getClientIDDatabase()), env
                                                                 .getClientStateDatabase());
    this.transactionPerisistor = new TransactionPersistorImpl(env.getTransactionDatabase(),
                                                              this.persistenceTransactionProvider);
    this.globalTransactionIDSequence = new SleepycatSequence(this.persistenceTransactionProvider, logger, 1, 1, env
        .getTransactionSequenceDatabase());
    this.classPersistor = new ClassPersistorImpl(this.persistenceTransactionProvider, logger, env.getClassDatabase());
    this.clusterStateStore = new SleepycatMapStore(this.persistenceTransactionProvider, logger, env
        .getClusterStateStoreDatabase());

  }

  private void open(DBEnvironment dbenv, TCLogger logger) throws TCDatabaseException {
    Assert.eval(!dbenv.isOpen());
    DatabaseOpenResult result = dbenv.open();
    if (!result.isClean()) { throw new DatabaseDirtyException(
                                                              "Attempt to open a dirty database.  "
                                                                  + "This may be because a previous instance of the server didn't exit cleanly."
                                                                  + "  Since the integrity of the data cannot be assured, "
                                                                  + "the server is refusing to start."
                                                                  + "  Please remove the database files in the following directory and restart "
                                                                  + "the server: " + dbenv.getEnvironmentHome()); }
  }

  private void sanityCheckAndClean(DBEnvironment dbenv, File l2DataPath, TCLogger logger) throws TCDatabaseException {
    PersistenceTransactionProvider persistentTxProvider = new SleepycatPersistenceTransactionProvider(dbenv
        .getEnvironment());
    PersistentMapStore persistentMapStore = new SleepycatMapStore(persistentTxProvider, logger, dbenv
        .getClusterStateStoreDatabase());

    // check for DBversion mismatch
    DBVersionChecker dbVersionChecker = new DBVersionChecker(persistentMapStore);
    dbVersionChecker.versionCheck();

    // RMP-309 : Allow passive L2s with dirty database come up automatically
    DirtyObjectDbCleaner dirtyObjectDbCleaner = new DirtyObjectDbCleaner(persistentMapStore, l2DataPath, logger);
    if (dirtyObjectDbCleaner.isObjectDbDirty()) {
      boolean dirtyDbAutoDelete = TCPropertiesImpl.getProperties()
          .getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE);

      if (!dirtyDbAutoDelete) {
        String errorMessage = Banner
            .makeBanner("Detected Dirty Objectdb. Auto-delete(l2.nha.dirtydb.autoDelete) not enabled. "
                        + "Please clean up the data directory and make sure that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server. It is important that the "
                        + StateManager.ACTIVE_COORDINATOR.getName()
                        + " is up and running before starting this server else you might end up losing data", "ERROR");
        throw new TCDatabaseException(errorMessage);
      } else {
        logger.info("Dirty Objectdb Auto-delete requested.");
      }

      close();
      dirtyObjectDbCleaner.backupDirtyObjectDb();
      open(dbenv, logger);
    }
  }

  public StringIndex getStringIndex() {
    return this.stringIndex;
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return this.persistenceTransactionProvider;
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    return this.globalTransactionIDSequence;
  }

  public TransactionPersistor getTransactionPersistor() {
    return this.transactionPerisistor;
  }

  public ManagedObjectPersistor getManagedObjectPersistor() {
    return this.managedObjectPersistor;
  }

  public ClientStatePersistor getClientStatePersistor() {
    return this.clientStatePersistor;
  }

  public ClassPersistor getClassPersistor() {
    return this.classPersistor;
  }

  public PersistentCollectionFactory getPersistentCollectionFactory() {
    return this.sleepycatCollectionFactory;
  }

  public PersistentMapStore getClusterStateStore() {
    return clusterStateStore;
  }

  public void close() {
    try {
      env.close();
    } catch (TCDatabaseException e) {
      throw new DBException(e);
    }
  }

  public boolean isOpen() {
    return env.isOpen();
  }

  static class SleepycatPersistorBase {

    private static final TCLogger logger = TCLogging.getLogger(SleepycatPersistorBase.class);

    protected Transaction pt2nt(PersistenceTransaction tx) {
      // XXX: Yuck.
      return (tx instanceof TransactionWrapper) ? ((TransactionWrapper) tx).getTransaction() : null;
    }

    protected void abortOnError(PersistenceTransaction ptx) {
      abortOnError(pt2nt(ptx));
    }

    protected void abortOnError(Transaction tx) {
      try {
        if (tx != null) tx.abort();
      } catch (DatabaseException e) {
        // This doesn't throw an exception as we don't want to create a Red herring.
        logger.error("Error on abortOnError", e);
      }

    }

    protected void abortOnError(Cursor cursor, PersistenceTransaction ptx) {
      abortOnError(cursor, pt2nt(ptx));
    }

    protected void abortOnError(Cursor cursor, Transaction tx) {
      if (cursor != null) {
        try {
          cursor.close();
        } catch (DatabaseException e) {
          // This doesn't throw an exception as we don't want to create a Red herring.
          logger.error("Error on abortOnError", e);
        }
      }
      abortOnError(tx);
    }

  }

  /**
   * This is only exposed for tests.
   */
  public SerializationAdapter getSerializationAdapter() throws IOException {
    return this.managedObjectPersistor.getSerializationAdapter();
  }

  /**
   * This is only exposed for tests.
   */
  public SleepycatCollectionsPersistor getCollectionsPersistor() {
    return sleepycatCollectionsPersistor;
  }

}
