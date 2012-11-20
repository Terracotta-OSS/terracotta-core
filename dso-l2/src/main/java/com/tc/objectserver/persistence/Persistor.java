package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class Persistor {
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final StorageManager storageManager;

  private volatile boolean started = false;

  private final PersistentMapStore persistentMapStore;

  private TransactionPersistor transactionPersistor;
  private ManagedObjectPersistor managedObjectPersistor;
  private MutableSequence gidSequence;
  private ClientStatePersistor clientStatePersistor;
  private SequenceManager sequenceManager;
  private PersistenceTransactionProvider persistenceTransactionProvider;
  private ObjectIDSetMaintainer objectIDSetMaintainer;
  private PersistentObjectFactory persistentObjectFactory;

  public Persistor(StorageManagerFactory storageManagerFactory) {
    objectIDSetMaintainer = new ObjectIDSetMaintainer();
    try {
      storageManager = storageManagerFactory.createStorageManager(getDataStorageConfigs(),
          new SingletonTransformerLookup(Object.class, LiteralSerializer.INSTANCE));
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    persistentMapStore = new PersistentMapStoreImpl(storageManager);
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getDataStorageConfigs() {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    TransactionPersistor.addConfigsTo(configs);
    ClientStatePersistor.addConfigsTo(configs);
    ManagedObjectPersistor.addConfigsTo(configs, objectIDSetMaintainer);
    SequenceManager.addConfigsTo(configs);
    return configs;
  }

  public void start() {
    try {
      storageManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new SequenceManager(storageManager);
    transactionPersistor = new TransactionPersistor(storageManager);
    clientStatePersistor = new ClientStatePersistor(sequenceManager, storageManager);
    managedObjectPersistor = new ManagedObjectPersistor(storageManager, sequenceManager, objectIDSetMaintainer);
    persistenceTransactionProvider = new PersistenceTransactionProvider(storageManager);
    persistentObjectFactory = new PersistentObjectFactory(storageManager);

    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);

    started = true;
  }

  public void close() {
    checkStarted();
    storageManager.close();
  }
  
  public MonitoredResource getMonitoredResource() {
    checkStarted();
    Collection<MonitoredResource> list = storageManager.getMonitoredResources();
    for (MonitoredResource rsrc : list) {
      if (rsrc.getType() == MonitoredResource.Type.OFFHEAP || rsrc.getType() == MonitoredResource.Type.HEAP) {
        return rsrc;
      }
    }
    return null;
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    checkStarted();
    return persistenceTransactionProvider;
  }

  public ClientStatePersistor getClientStatePersistor() {
    checkStarted();
    return clientStatePersistor;
  }

  public ManagedObjectPersistor getManagedObjectPersistor() {
    checkStarted();
    return managedObjectPersistor;
  }

  public TransactionPersistor getTransactionPersistor() {
    checkStarted();
    return transactionPersistor;
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    checkStarted();
    return gidSequence;
  }

  public PersistentMapStore getPersistentStateStore() {
    return persistentMapStore;
  }

  public PersistentObjectFactory getPersistentObjectFactory() {
    checkStarted();
    return persistentObjectFactory;
  }

  public SequenceManager getSequenceManager() {
    checkStarted();
    return sequenceManager;
  }

  private void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Persistor is not yet started.");
    }
  }
}
