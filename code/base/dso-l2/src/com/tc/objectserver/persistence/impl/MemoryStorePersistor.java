/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.io.serializer.api.StringIndex;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.memorydatastore.client.MemoryDataStoreClient;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.sequence.MutableSequence;

public class MemoryStorePersistor implements Persistor {
  private static final String                   ROOT_DB_NAME            = "roots";
  private static final String                   OBJECT_DB_NAME          = "objects";
  private static final String                   TRANSACTION_DB_NAME     = "transactions";
  private static final String                   CLUSTER_STATE_STORE     = "clusterstatestore";
  private static final String                   MAP_DB_NAME             = "mapsdatabase";

  private final ClientStatePersistor            clientStatePersistor;
  private final StringIndex                     stringIndex;
  private final ManagedObjectPersistor          managedObjectPersistor;
  private final ClassPersistor                  clazzPersistor;
  private final PersistentMapStore              persistentStateStore;
  private final TransactionPersistor            transactionPerisistor;
  private final MutableSequence                 mutableSequence;
  private final PersistenceTransactionProvider  persistenceTransactionProvider;
  private final MemoryStoreCollectionFactory    memoryStoreCollectionFactory;
  private final MemoryStoreCollectionsPersistor memoryStoreCollectionsPersistor;

  private final String                          PropertyMemoryStoreHost = "l2.memorystore.host";
  private final String                          PropertyMemoryStorePort = "l2.memorystore.port";
  private final String                          memoryStoreHost;
  private final int                             memoryStorePort;

  public MemoryStorePersistor() {
    this(TCLogging.getLogger(MemoryStorePersistor.class));
  }

  public MemoryStorePersistor(final TCLogger logger) {
    this.memoryStoreHost = TCPropertiesImpl.getProperties().getProperty(this.PropertyMemoryStoreHost);
    this.memoryStorePort = TCPropertiesImpl.getProperties().getInt(this.PropertyMemoryStorePort);

    this.persistenceTransactionProvider = new NullPersistenceTransactionProvider();
    this.clientStatePersistor = new InMemoryClientStatePersistor();
    this.stringIndex = new StringIndexImpl(new NullStringIndexPersistor());
    this.clazzPersistor = new InMemoryClassPersistor();
    this.memoryStoreCollectionFactory = new MemoryStoreCollectionFactory();
    final MemoryDataStoreClient mapsDB = new MemoryDataStoreClient(MAP_DB_NAME, this.memoryStoreHost,
                                                                   this.memoryStorePort);
    this.memoryStoreCollectionsPersistor = new MemoryStoreCollectionsPersistor(logger, mapsDB,
                                                                               this.memoryStoreCollectionFactory);
    this.memoryStoreCollectionFactory.setMemoryDataStore(mapsDB);
    this.memoryStoreCollectionFactory.setPersistor(this.memoryStoreCollectionsPersistor);

    final MemoryDataStoreClient objectDB = new MemoryDataStoreClient(OBJECT_DB_NAME, this.memoryStoreHost,
                                                                     this.memoryStorePort);
    final MemoryDataStoreClient rootDB = new MemoryDataStoreClient(ROOT_DB_NAME, this.memoryStoreHost,
                                                                   this.memoryStorePort);
    this.managedObjectPersistor = new MemoryStoreManagedObjectPersistor(logger, objectDB,
                                                                        new InMemorySequenceProvider(), rootDB,
                                                                        this.memoryStoreCollectionsPersistor);
    final MemoryDataStoreClient transactionStore = new MemoryDataStoreClient(TRANSACTION_DB_NAME, this.memoryStoreHost,
                                                                             this.memoryStorePort);
    this.transactionPerisistor = new MemoryStoreTransactionPersistor(transactionStore);
    this.mutableSequence = new InMemorySequenceProvider();
    final MemoryDataStoreClient clusterStateDB = new MemoryDataStoreClient(CLUSTER_STATE_STORE, this.memoryStoreHost,
                                                                           this.memoryStorePort);
    this.persistentStateStore = new MemoryStorePersistentMapStore(clusterStateDB);
  }

  public void close() {
    return;
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return this.persistenceTransactionProvider;
  }

  public ClientStatePersistor getClientStatePersistor() {
    return this.clientStatePersistor;
  }

  public ManagedObjectPersistor getManagedObjectPersistor() {
    return this.managedObjectPersistor;
  }

  public TransactionPersistor getTransactionPersistor() {
    return this.transactionPerisistor;
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    return this.mutableSequence;
  }

  public StringIndex getStringIndex() {
    return this.stringIndex;
  }

  public ClassPersistor getClassPersistor() {
    return this.clazzPersistor;
  }

  public PersistentCollectionFactory getPersistentCollectionFactory() {
    return this.memoryStoreCollectionFactory;
  }

  public PersistentMapStore getPersistentStateStore() {
    return this.persistentStateStore;
  }

}
