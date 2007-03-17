/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.io.serializer.api.StringIndex;
import com.tc.objectserver.persistence.api.ClassPersistor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.PersistentSequence;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;

public class InMemoryPersistor implements Persistor {

  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final ClientStatePersistor           clientStatePersistor;
  private final StringIndex                    stringIndex;
  private final ClassPersistor                 clazzPersistor;
  private final PersistentCollectionFactory    persistentCollectionFactory;
  private final PersistentMapStore             clusterStateStore;

  public InMemoryPersistor() {
    this.persistenceTransactionProvider = new NullPersistenceTransactionProvider();
    this.clientStatePersistor = new InMemoryClientStatePersistor();
    this.stringIndex = new StringIndexImpl(new NullStringIndexPersistor());
    this.clazzPersistor = new InMemoryClassPersistor();
    this.persistentCollectionFactory = new InMemoryCollectionFactory();
    this.clusterStateStore = new InMemoryPersistentMapStore();

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
    throw new ImplementMe();
  }

  public TransactionPersistor getTransactionPersistor() {
    throw new ImplementMe();
  }

  public PersistentSequence getGlobalTransactionIDSequence() {
    throw new ImplementMe();
  }

  public StringIndex getStringIndex() {
    return stringIndex;
  }

  public ClassPersistor getClassPersistor() {
    return this.clazzPersistor;
  }

  public PersistentCollectionFactory getPersistentCollectionFactory() {
    return persistentCollectionFactory;
  }

  public PersistentMapStore getClusterStateStore() {
    return clusterStateStore;
  }

}
