package com.tc.objectserver.persistence.gb;

import com.tc.io.serializer.api.StringIndex;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.persistence.api.*;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.sequence.MutableSequence;

/**
 * @author tim
 */
public class GBPersistor implements Persistor {

  private final GBTransactionPersistor transactionPersistor = new GBTransactionPersistor();
  private final GBManagedObjectPersistor managedObjectPersistor = new GBManagedObjectPersistor();
  private final GBSequence gidSequence = new GBSequence(null, null);
  private final GBClientStatePersistor clientStatePersistor = new GBClientStatePersistor();
  private final GBPersistentMapStore persistentMapStore = new GBPersistentMapStore();

  @Override
  public void close() {
  }

  @Override
  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return null;
  }

  @Override
  public ClientStatePersistor getClientStatePersistor() {
    return clientStatePersistor;
  }

  @Override
  public ManagedObjectPersistor getManagedObjectPersistor() {
    return managedObjectPersistor;
  }

  @Override
  public TransactionPersistor getTransactionPersistor() {
    return transactionPersistor;
  }

  @Override
  public MutableSequence getGlobalTransactionIDSequence() {
    return gidSequence;
  }

  @Override
  public ClassPersistor getClassPersistor() {
    return null;
  }

  @Override
  public StringIndex getStringIndex() {
    return null;
  }

  @Override
  public PersistentCollectionFactory getPersistentCollectionFactory() {
    return null;
  }

  @Override
  public PersistentMapStore getPersistentStateStore() {
    return persistentMapStore;
  }
}
