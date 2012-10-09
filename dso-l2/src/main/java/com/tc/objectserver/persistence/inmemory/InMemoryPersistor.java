/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.inmemory;

import com.tc.exception.ImplementMe;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.sequence.MutableSequence;

public class InMemoryPersistor implements Persistor {

  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final ClientStatePersistor           clientStatePersistor;
  private final PersistentMapStore             persistentStateStore;

  public InMemoryPersistor() {
    this.persistenceTransactionProvider = new NullPersistenceTransactionProvider();
    this.clientStatePersistor = new InMemoryClientStatePersistor();
    this.persistentStateStore = new InMemoryPersistentMapStore();

  }

  public void close() {
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
    return new NullTransactionPersistor();
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    return new InMemorySequenceProvider();
  }

  public PersistentMapStore getPersistentStateStore() {
    return persistentStateStore;
  }

}
