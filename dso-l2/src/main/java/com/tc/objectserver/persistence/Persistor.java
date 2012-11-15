package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.util.sequence.MutableSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class Persistor {

  private static final String TRANSACTION = "transaction";
  private static final String CLIENT_STATES = "client_states";
  private static final String OBJECT_DB = "object_db";
  private static final String ROOT_DB = "root_db";
  private static final String STATE_MAP = "state_map";
  private static final String SEQUENCE_MAP = "sequence_map";
  private static final String SEQUENCE_UUID_MAP = "sequence_uuid_map";

  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String OBJECT_ID_SEQUENCE = "object_id_sequence";
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final StorageManagerFactory storageManagerFactory;
  private final StorageManager metadataStorageManager;

  private volatile boolean started = false;

  private StorageManager dataStorageManager;
  private TransactionPersistor transactionPersistor;
  private ManagedObjectPersistor managedObjectPersistor;
  private MutableSequence gidSequence;
  private ClientStatePersistor clientStatePersistor;
  private PersistentMapStoreImpl persistentMapStore;
  private SequenceManager sequenceManager;
  private PersistenceTransactionProvider persistenceTransactionProvider;
  private ObjectIDSetMaintainer objectIDSetMaintainer;
  private PersistentObjectFactory persistentObjectFactory;

  public Persistor(StorageManagerFactory storageManagerFactory) {
    objectIDSetMaintainer = new ObjectIDSetMaintainer();
    try {
      metadataStorageManager = storageManagerFactory.createMetadataStorageManager(getMetadataStorageConfigs());
      metadataStorageManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    persistentMapStore = new PersistentMapStoreImpl(metadataStorageManager.getKeyValueStorage(STATE_MAP, String.class, String.class));
    this.storageManagerFactory = storageManagerFactory;
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getMetadataStorageConfigs() {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    configs.put(STATE_MAP, PersistentMapStoreImpl.config());
    return configs;
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getDataStorageConfigs() {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    configs.put(TRANSACTION, TransactionPersistor.config());
    configs.put(CLIENT_STATES, ClientStatePersistor.config());
    configs.put(ROOT_DB, ManagedObjectPersistor.rootMapConfig());
    configs.put(SEQUENCE_MAP, SequenceManager.sequenceMapConfig());
    configs.put(SEQUENCE_UUID_MAP, SequenceManager.uuidMapConfig());
    configs.put(OBJECT_DB, ManagedObjectPersistor.objectConfig(objectIDSetMaintainer));
    return configs;
  }

  public void start() {
    try {
      dataStorageManager = storageManagerFactory.createStorageManager(getDataStorageConfigs(),
          new SingletonTransformerLookup(Object.class, LiteralSerializer.INSTANCE));
      dataStorageManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new SequenceManager(dataStorageManager.getKeyValueStorage(SEQUENCE_MAP, String.class, Long.class),
        dataStorageManager.getKeyValueStorage(SEQUENCE_UUID_MAP, String.class, String.class));
    transactionPersistor = new TransactionPersistor(dataStorageManager.getKeyValueStorage(TRANSACTION,
        GlobalTransactionID.class,
        GlobalTransactionDescriptor.class));
    clientStatePersistor = new ClientStatePersistor(sequenceManager.getSequence(CLIENT_STATE_SEQUENCE), dataStorageManager
        .getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class));
    managedObjectPersistor = new ManagedObjectPersistor(dataStorageManager.getKeyValueStorage(ROOT_DB, String.class, ObjectID.class), dataStorageManager
        .getKeyValueStorage(OBJECT_DB, Long.class, byte[].class), sequenceManager.getSequence(OBJECT_ID_SEQUENCE), objectIDSetMaintainer);
    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);
    persistenceTransactionProvider = new PersistenceTransactionProvider(dataStorageManager);
    persistentObjectFactory = new PersistentObjectFactory(dataStorageManager);

    started = true;
  }

  public void close() {
    checkStarted();
    dataStorageManager.shutdown();
    metadataStorageManager.shutdown();
  }
  
  public MonitoredResource getMonitoredResource() {
    checkStarted();
    Collection<MonitoredResource> list = dataStorageManager.getMonitoredResources();
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
