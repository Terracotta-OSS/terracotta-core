package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.util.sequence.MutableSequence;

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

  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String OBJECT_ID_SEQUENCE = "object_id_sequence";
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final StorageManager storageManager;

  private final TransactionPersistor transactionPersistor;
  private final ManagedObjectPersistor managedObjectPersistor;
  private final MutableSequence gidSequence;
  private final ClientStatePersistor clientStatePersistor;
  private final PersistentMapStoreImpl persistentMapStore;
  private final SequenceManager sequenceManager;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final ObjectIDSetMaintainer objectIDSetMaintainer;
  private final PersistentObjectFactory persistentObjectFactory;

  public Persistor(StorageManagerFactory storageManagerFactory) {
    objectIDSetMaintainer = new ObjectIDSetMaintainer();
    try {
      storageManager = storageManagerFactory.createStorageManager(getCoreStorageConfig());
      storageManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new SequenceManager(storageManager.getKeyValueStorage(SEQUENCE_MAP, String.class, Long.class));
    transactionPersistor = new TransactionPersistor(storageManager.getKeyValueStorage(TRANSACTION,
        GlobalTransactionID.class,
        GlobalTransactionDescriptor.class));
    persistentMapStore = new PersistentMapStoreImpl(storageManager.getKeyValueStorage(STATE_MAP, String.class, String.class));
    clientStatePersistor = new ClientStatePersistor(sequenceManager.getSequence(CLIENT_STATE_SEQUENCE), storageManager.getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class));
    managedObjectPersistor = new ManagedObjectPersistor(storageManager.getKeyValueStorage(ROOT_DB, String.class, ObjectID.class), storageManager
        .getKeyValueStorage(OBJECT_DB, Long.class, byte[].class), sequenceManager.getSequence(OBJECT_ID_SEQUENCE), objectIDSetMaintainer);
    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);
    persistenceTransactionProvider = new PersistenceTransactionProvider(storageManager);
    persistentObjectFactory = new PersistentObjectFactory(storageManager);
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getCoreStorageConfig() {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    configs.put(TRANSACTION, TransactionPersistor.config());
    configs.put(CLIENT_STATES, ClientStatePersistor.config());
    configs.put(ROOT_DB, ManagedObjectPersistor.rootMapConfig());
    configs.put(STATE_MAP, PersistentMapStoreImpl.config());
    configs.put(SEQUENCE_MAP, SequenceManager.config());
    configs.put(OBJECT_DB, ManagedObjectPersistor.objectConfig(objectIDSetMaintainer));
    return configs;
  }

  public void close() {
    storageManager.shutdown();
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return persistenceTransactionProvider;
  }

  public ClientStatePersistor getClientStatePersistor() {
    return clientStatePersistor;
  }

  public ManagedObjectPersistor getManagedObjectPersistor() {
    return managedObjectPersistor;
  }

  public TransactionPersistor getTransactionPersistor() {
    return transactionPersistor;
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    return gidSequence;
  }

  public PersistentMapStore getPersistentStateStore() {
    return persistentMapStore;
  }

  public PersistentObjectFactory getPersistentObjectFactory() {
    return persistentObjectFactory;
  }

  public SequenceManager getSequenceManager() {
    return sequenceManager;
  }
}
