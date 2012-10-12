package com.tc.objectserver.persistence.gb;

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
public class GBPersistor {

  private static final String TRANSACTION = "transaction";
  private static final String CLIENT_STATES = "client_states";
  private static final String OBJECT_DB = "object_db";
  private static final String ROOT_DB = "root_db";
  private static final String STATE_MAP = "state_map";
  private static final String SEQUENCE_MAP = "sequence_map";

  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String OBJECT_ID_SEQUENCE = "object_id_sequence";
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final StorageManager gbManager;

  private final GBTransactionPersistor transactionPersistor;
  private final GBManagedObjectPersistor managedObjectPersistor;
  private final GBSequence gidSequence;
  private final GBClientStatePersistor clientStatePersistor;
  private final GBPersistentMapStore persistentMapStore;
  private final GBSequenceManager sequenceManager;
  private final GBPersistenceTransactionProvider persistenceTransactionProvider;
  private final GBObjectIDSetMaintainer objectIDSetMaintainer;
  private final GBPersistentObjectFactory persistentObjectFactory;

  public GBPersistor(StorageManagerFactory storageManagerFactory) {
    objectIDSetMaintainer = new GBObjectIDSetMaintainer();
    gbManager = storageManagerFactory.createStorageManager(getCoreStorageConfig());
    try {
      gbManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new GBSequenceManager(gbManager.getKeyValueStorage(SEQUENCE_MAP, String.class, Long.class));
    transactionPersistor = new GBTransactionPersistor(gbManager.getKeyValueStorage(TRANSACTION,
        GlobalTransactionID.class,
        GlobalTransactionDescriptor.class));
    persistentMapStore = new GBPersistentMapStore(gbManager.getKeyValueStorage(STATE_MAP, String.class, String.class));
    clientStatePersistor = new GBClientStatePersistor(sequenceManager.getSequence(CLIENT_STATE_SEQUENCE), gbManager.getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class));
    managedObjectPersistor = new GBManagedObjectPersistor(gbManager.getKeyValueStorage(ROOT_DB, String.class, ObjectID.class), gbManager
        .getKeyValueStorage(OBJECT_DB, Long.class, byte[].class), sequenceManager.getSequence(OBJECT_ID_SEQUENCE), objectIDSetMaintainer);
    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);
    persistenceTransactionProvider = new GBPersistenceTransactionProvider(gbManager);
    persistentObjectFactory = new GBPersistentObjectFactory(gbManager);
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getCoreStorageConfig() {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    configs.put(TRANSACTION, GBTransactionPersistor.config());
    configs.put(CLIENT_STATES, GBClientStatePersistor.config());
    configs.put(ROOT_DB, GBManagedObjectPersistor.rootMapConfig());
    configs.put(STATE_MAP, GBPersistentMapStore.config());
    configs.put(SEQUENCE_MAP, GBSequenceManager.config());
    KeyValueStorageConfig<Long, byte[]> objectDbConfig = GBManagedObjectPersistor.objectConfig();
    objectDbConfig.addListener(objectIDSetMaintainer);
    configs.put(OBJECT_DB, objectDbConfig);
    return configs;
  }

  public void close() {
    gbManager.shutdown();
  }

  public GBPersistenceTransactionProvider getPersistenceTransactionProvider() {
    return persistenceTransactionProvider;
  }

  public GBClientStatePersistor getClientStatePersistor() {
    return clientStatePersistor;
  }

  public GBManagedObjectPersistor getManagedObjectPersistor() {
    return managedObjectPersistor;
  }

  public GBTransactionPersistor getTransactionPersistor() {
    return transactionPersistor;
  }

  public MutableSequence getGlobalTransactionIDSequence() {
    return gidSequence;
  }

  public PersistentMapStore getPersistentStateStore() {
    return persistentMapStore;
  }

  public GBPersistentObjectFactory getPersistentObjectFactory() {
    return persistentObjectFactory;
  }

  public GBSequenceManager getSequenceManager() {
    return sequenceManager;
  }
}
