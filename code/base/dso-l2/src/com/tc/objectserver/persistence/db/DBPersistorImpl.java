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
  private final PersistableCollectionFactory   collectionFactory;
  private final PersistentMapStore             persistentStateStore;

  private final TCCollectionsPersistor         collectionsPersistor;

  // only for tests
  public DBPersistorImpl(final TCLogger logger, final DBEnvironment env,
                         final SerializationAdapterFactory serializationAdapterFactory) throws TCDatabaseException {
    this(logger, env, serializationAdapterFactory, null, new ObjectStatsRecorder());
  }

  public DBPersistorImpl(final TCLogger logger, final DBEnvironment env,
                         final SerializationAdapterFactory serializationAdapterFactory, final File l2DataPath,
                         final ObjectStatsRecorder objectStatsRecorder) throws TCDatabaseException {

    open(env, logger);
    this.env = env;

    sanityCheckAndClean(env, l2DataPath, logger);

    this.persistenceTransactionProvider = createPersistenceTransactionProvider(env);
    this.stringIndexPersistor = createStringIndexPersistor();
    this.classPersistor = createClassPersistor(logger);
    this.persistentStateStore = createPersistentMapStore(logger);
    this.clientStatePersistor = createClientStatePersistor(logger);
    this.transactionPerisistor = createTransactionPersistor();
    this.globalTransactionIDSequence = createGlobalTransactionIDSequence(logger);

    this.stringIndex = new StringIndexImpl(this.stringIndexPersistor, DEFAULT_CAPACITY);
    final TCCollectionsSerializer serializer = new TCCollectionsSerializerImpl();
    this.collectionFactory = new PersistableCollectionFactory(env.getMapsDatabase().getBackingMapFactory(serializer),
                                                              env.isParanoidMode());
    this.collectionsPersistor = new TCCollectionsPersistor(logger, env.getMapsDatabase(), this.collectionFactory,
                                                           serializer);
    this.managedObjectPersistor = new ManagedObjectPersistorImpl(logger, serializationAdapterFactory, env,
                                                                 env.getSequence(this.persistenceTransactionProvider,
                                                                                 logger,
                                                                                 DBSequenceKeys.OBJECTID_SEQUENCE_NAME,
                                                                                 1000), env.getRootDatabase(),
                                                                 this.persistenceTransactionProvider,
                                                                 this.collectionsPersistor, env.isParanoidMode(),
                                                                 objectStatsRecorder);
  }

  protected PersistenceTransactionProvider createPersistenceTransactionProvider(final DBEnvironment dbenv) {
    return dbenv.getPersistenceTransactionProvider();
  }

  protected StringIndexPersistor createStringIndexPersistor() throws TCDatabaseException {
    return new StringIndexPersistorImpl(this.persistenceTransactionProvider, env.getStringIndexDatabase());
  }

  protected ClassPersistor createClassPersistor(final TCLogger logger) throws TCDatabaseException {
    return new ClassPersistorImpl(this.persistenceTransactionProvider, logger, env.getClassDatabase());
  }

  protected PersistentMapStore createPersistentMapStore(final TCLogger logger) throws TCDatabaseException {
    return new TCMapStore(this.persistenceTransactionProvider, logger, env.getClusterStateStoreDatabase());
  }

  protected ClientStatePersistor createClientStatePersistor(final TCLogger logger) throws TCDatabaseException {
    return new ClientStatePersistorImpl(logger, this.persistenceTransactionProvider,
                                        env.getSequence(this.persistenceTransactionProvider, logger,
                                                        DBSequenceKeys.CLIENTID_SEQUENCE_NAME, 0),
                                        env.getClientStateDatabase());
  }

  protected TransactionPersistor createTransactionPersistor() throws TCDatabaseException {
    return new TransactionPersistorImpl(env.getTransactionDatabase(), this.persistenceTransactionProvider);
  }

  protected MutableSequence createGlobalTransactionIDSequence(final TCLogger logger) {
    return env.getSequence(this.persistenceTransactionProvider, logger, DBSequenceKeys.TRANSACTION_SEQUENCE_DB_NAME, 1);
  }

  private void open(final DBEnvironment dbenv, final TCLogger logger) throws TCDatabaseException {
    Assert.eval(!dbenv.isOpen());
    final boolean result = dbenv.open();
    if (!result) { throw new DatabaseDirtyException(
                                                    "Attempt to open a dirty database.  "
                                                        + "This may be because a previous instance of the server didn't exit cleanly."
                                                        + "  Since the integrity of the data cannot be assured, "
                                                        + "the server is refusing to start."
                                                        + "  Please remove the database files in the following directory and restart "
                                                        + "the server: " + dbenv.getEnvironmentHome()); }
  }

  private void sanityCheckAndClean(final DBEnvironment dbenv, final File l2DataPath, final TCLogger logger)
      throws TCDatabaseException {
    final PersistenceTransactionProvider persistentTxProvider = dbenv.getPersistenceTransactionProvider();
    final PersistentMapStore persistentMapStore = new TCMapStore(persistentTxProvider, logger,
                                                                 dbenv.getClusterStateStoreDatabase());

    // check for DBversion mismatch
    final DBVersionChecker dbVersionChecker = new DBVersionChecker(persistentMapStore);
    dbVersionChecker.versionCheck();

    // RMP-309 : Allow passive L2s with dirty database come up automatically
    final DirtyObjectDbCleaner dirtyObjectDbCleaner = new DirtyObjectDbCleaner(persistentMapStore, l2DataPath, logger);
    if (dirtyObjectDbCleaner.isObjectDbDirty()) {
      final boolean dirtyDbAutoDelete = TCPropertiesImpl.getProperties()
          .getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE);

      if (!dirtyDbAutoDelete) {
        final String errorMessage = Banner
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
    return this.collectionFactory;
  }

  public PersistentMapStore getPersistentStateStore() {
    return this.persistentStateStore;
  }

  public void close() {
    try {
      if (this.managedObjectPersistor != null) {
        // If starting up from a passive with a dirty db, it's possible to
        // get a close() call (from sanityCheckAndClean()) before managedObjectPersistor
        // is created.
        this.managedObjectPersistor.close();
      }
      this.env.close();
    } catch (final TCDatabaseException e) {
      throw new DBException(e);
    }
  }

  public boolean isOpen() {
    return this.env.isOpen();
  }

  static class DBPersistorBase {

    private static final TCLogger logger = TCLogging.getLogger(DBPersistorBase.class);

    protected void abortOnError(final PersistenceTransaction tx) {
      try {
        if (tx != null) {
          tx.abort();
        }
      } catch (final Exception e) {
        // This doesn't throw an exception as we don't want to create a Red herring.
        logger.error("Error on abortOnError", e);
      }

    }

    protected void abortOnError(final TCDatabaseCursor cursor, final PersistenceTransaction tx) {
      if (cursor != null) {
        try {
          cursor.close();
        } catch (final Exception e) {
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
    return this.collectionsPersistor;
  }

}
