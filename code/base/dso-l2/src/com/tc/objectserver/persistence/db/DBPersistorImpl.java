/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.io.serializer.api.StringIndex;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.inmemory.StringIndexImpl;
import com.tc.objectserver.storage.api.DBEnvironment;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCDatabaseCursor;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.Banner;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.io.File;

public class DBPersistorImpl implements Persistor {
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
  private final PersistableCollectionFactory     sleepycatCollectionFactory;
  private final PersistentMapStore             persistentStateStore;

  private TCCollectionsPersistor        sleepycatCollectionsPersistor;

  // only for tests
  public DBPersistorImpl(TCLogger logger, DBEnvironment env, SerializationAdapterFactory serializationAdapterFactory)
      throws TCDatabaseException {
    this(logger, env, serializationAdapterFactory, null, new ObjectStatsRecorder());
  }

  public DBPersistorImpl(TCLogger logger, DBEnvironment env,
                            SerializationAdapterFactory serializationAdapterFactory, File l2DataPath,
                            ObjectStatsRecorder objectStatsRecorder) throws TCDatabaseException {

    open(env, logger);
    this.env = env;

    sanityCheckAndClean(env, l2DataPath, logger);

    this.persistenceTransactionProvider = env.getPersistenceTransactionProvider();
    this.stringIndexPersistor = new StringIndexPersistorImpl(persistenceTransactionProvider, env
        .getStringIndexDatabase());
    this.stringIndex = new StringIndexImpl(this.stringIndexPersistor, DEFAULT_CAPACITY);
    this.sleepycatCollectionFactory = new PersistableCollectionFactory();
    this.sleepycatCollectionsPersistor = new TCCollectionsPersistor(logger, env.getMapsDatabase(),
                                                                           sleepycatCollectionFactory);
    this.managedObjectPersistor = new ManagedObjectPersistorImpl(logger,

    serializationAdapterFactory, env, env.getSequence(this.persistenceTransactionProvider, logger,
                                                      DBSequenceKeys.OBJECTID_SEQUENCE_NAME, 1000), env
        .getRootDatabase(), this.persistenceTransactionProvider, this.sleepycatCollectionsPersistor, env
        .isParanoidMode(), objectStatsRecorder);
    this.clientStatePersistor = new ClientStatePersistorImpl(logger, this.persistenceTransactionProvider, env
        .getSequence(this.persistenceTransactionProvider, logger, DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 0), env
        .getClientStateDatabase());
    this.transactionPerisistor = new TransactionPersistorImpl(env.getTransactionDatabase(),
                                                              this.persistenceTransactionProvider);
    this.globalTransactionIDSequence = env.getSequence(this.persistenceTransactionProvider, logger,
                                                       DBSequenceKeys.TRANSACTION_SEQUENCE_DB_NAME, 1);
    this.classPersistor = new ClassPersistorImpl(this.persistenceTransactionProvider, logger, env.getClassDatabase());
    this.persistentStateStore = new TCMapStore(this.persistenceTransactionProvider, logger, env
        .getClusterStateStoreDatabase());
  }

  private void open(DBEnvironment dbenv, TCLogger logger) throws TCDatabaseException {
    Assert.eval(!dbenv.isOpen());
    boolean result = dbenv.open();
    if (!result) { throw new DatabaseDirtyException(
                                                    "Attempt to open a dirty database.  "
                                                        + "This may be because a previous instance of the server didn't exit cleanly."
                                                        + "  Since the integrity of the data cannot be assured, "
                                                        + "the server is refusing to start."
                                                        + "  Please remove the database files in the following directory and restart "
                                                        + "the server: " + dbenv.getEnvironmentHome()); }
  }

  private void sanityCheckAndClean(DBEnvironment dbenv, File l2DataPath, TCLogger logger) throws TCDatabaseException {
    PersistenceTransactionProvider persistentTxProvider = dbenv.getPersistenceTransactionProvider();
    PersistentMapStore persistentMapStore = new TCMapStore(persistentTxProvider, logger, dbenv
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

  public PersistentMapStore getPersistentStateStore() {
    return persistentStateStore;
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

  static class DBPersistorBase {

    private static final TCLogger logger = TCLogging.getLogger(DBPersistorBase.class);

    protected void abortOnError(PersistenceTransaction tx) {
      try {
        if (tx != null) tx.abort();
      } catch (Exception e) {
        // This doesn't throw an exception as we don't want to create a Red herring.
        logger.error("Error on abortOnError", e);
      }

    }

    protected void abortOnError(TCDatabaseCursor cursor, PersistenceTransaction tx) {
      if (cursor != null) {
        try {
          cursor.close();
        } catch (Exception e) {
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
  public SerializationAdapter getSerializationAdapter() {
    return this.managedObjectPersistor.getSerializationAdapter();
  }

  /**
   * This is only exposed for tests.
   */
  public TCCollectionsPersistor getCollectionsPersistor() {
    return sleepycatCollectionsPersistor;
  }

}
