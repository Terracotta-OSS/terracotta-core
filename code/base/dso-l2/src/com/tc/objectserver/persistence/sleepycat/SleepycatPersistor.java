/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.io.serializer.api.StringIndex;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.impl.StringIndexImpl;

import java.io.IOException;

public class SleepycatPersistor implements Persistor {
  private static final int                     DEFAULT_CAPACITY = 50000;

  private final StringIndexPersistor           stringIndexPersistor;
  private final StringIndex                    stringIndex;
  private final ManagedObjectPersistorImpl     managedObjectPersistor;
  private final ClientStatePersistor           clientStatePersistor;
  private final TransactionPersistor           transactionPerisistor;
  private final PersistentSequence             globalTransactionIDSequence;
  private final ClassPersistor                 classPersistor;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final DBEnvironment                  env;
  private final SleepycatCollectionFactory     sleepycatCollectionFactory;

  private SleepycatCollectionsPersistor        sleepycatCollectionsPersistor;

  public SleepycatPersistor(TCLogger logger, DBEnvironment env, SerializationAdapterFactory serializationAdapterFactory)
      throws DatabaseException {
    DatabaseOpenResult result = env.open();
    if (!result.isClean()) {
      //
      throw new DatabaseDirtyException("Attempt to open a dirty database.  "
                                       + "This may be because a previous instance of the server didn't exit cleanly."
                                       + "  Since the integrity of the data cannot be assured, "
                                       + "the server is refusing to start."
                                       + "  Please remove the database files in the following directory and restart "
                                       + "the server: " + env.getEnvironmentHome());
    }

    CursorConfig dbCursorConfig = new CursorConfig();
    dbCursorConfig.setReadCommitted(true);
    CursorConfig rootDBCursorConfig = new CursorConfig();
    rootDBCursorConfig.setReadCommitted(true);
    CursorConfig stringIndexCursorConfig = new CursorConfig();
    stringIndexCursorConfig.setReadCommitted(true);
    this.env = env;
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
                                                                 env.getObjectDatabase(),
                                                                 dbCursorConfig,
                                                                 new SleepycatSequence(
                                                                                       this.persistenceTransactionProvider,
                                                                                       logger, 1, 1000, env
                                                                                           .getObjectIDDB()), env
                                                                     .getRootDatabase(), rootDBCursorConfig,
                                                                 this.persistenceTransactionProvider,
                                                                 this.sleepycatCollectionsPersistor);
    this.clientStatePersistor = new ClientStatePersistorImpl(logger, this.persistenceTransactionProvider,
                                                             new SleepycatSequence(this.persistenceTransactionProvider,
                                                                                   logger, 1, 0, env
                                                                                       .getClientIDDatabase()), env
                                                                 .getClientStateDatabase());
    this.transactionPerisistor = new TransactionPersistorImpl(env.getTransactionDatabase(), new SerialBinding(env
        .getClassCatalogWrapper().getClassCatalog(), ServerTransactionID.class), new SerialBinding(env
        .getClassCatalogWrapper().getClassCatalog(), GlobalTransactionDescriptor.class),
                                                              this.persistenceTransactionProvider);
    this.globalTransactionIDSequence = new SleepycatSequence(this.persistenceTransactionProvider, logger, 1, 1, env
        .getTransactionSequenceDatabase());
    this.classPersistor = new ClassPersistorImpl(this.persistenceTransactionProvider, logger, env.getClassDatabase());
  }

  public StringIndex getStringIndex() {
    return this.stringIndex;
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return this.persistenceTransactionProvider;
  }

  public PersistentSequence getGlobalTransactionIDSequence() {
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

  public void close() {
    try {
      env.close();
    } catch (DatabaseException e) {
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
        if(tx != null) tx.abort();
      } catch (DatabaseException e) {
        // This doesnt throw an exception as we dont want to create a Red herring.
        logger.error("Error on abortOnError", e);
      }
      
    }

    protected void abortOnError(Cursor cursor, PersistenceTransaction ptx) {
     abortOnError(cursor, pt2nt(ptx)); 
    }
    
    protected void abortOnError(Cursor cursor, Transaction tx) {
      if(cursor != null) {
        try {
          cursor.close();
        } catch (DatabaseException e) {
        // This doesnt throw an exception as we dont want to create a Red herring.
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
