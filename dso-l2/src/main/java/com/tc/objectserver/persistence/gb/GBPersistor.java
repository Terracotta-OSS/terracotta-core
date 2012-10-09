package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.GBManager;
import com.tc.gbapi.GBManagerConfiguration;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapFactory;
import com.tc.gbapi.impl.GBOnHeapMapFactory;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.util.sequence.MutableSequence;

import java.io.File;

/**
 * @author tim
 */
public class GBPersistor implements Persistor {

  private static final String TRANSACTION = "transaction";
  private static final String CLIENT_STATES = "client_states";
  private static final String OBJECT_DB = "object_db";
  private static final String ROOT_DB = "root_db";
  private static final String STATE_MAP = "state_map";
  private static final String SEQUENCE_MAP = "sequence_map";

  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String OBJECT_ID_SEQUENCE = "object_id_sequence";
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final GBMapFactory mapFactory;
  private final GBManager gbManager;

  private final GBTransactionPersistor transactionPersistor;
  private final GBManagedObjectPersistor managedObjectPersistor;
  private final GBSequence gidSequence;
  private final GBClientStatePersistor clientStatePersistor;
  private final GBPersistentMapStore persistentMapStore;
  private final GBSequenceManager sequenceManager;
  private final GBPersistenceTransactionProvider persistenceTransactionProvider;
  private final GBObjectIDSetMaintainer objectIDSetMaintainer;
  private final GBPersistentObjectFactory persistentObjectFactory;

  public GBPersistor(File path) {
    objectIDSetMaintainer = new GBObjectIDSetMaintainer();
    mapFactory = new GBOnHeapMapFactory();
    gbManager = new GBManager(path, mapFactory);
    verifyOrCreate(gbManager);
    try {
      gbManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new GBSequenceManager(gbManager.getMap(SEQUENCE_MAP, String.class, Long.class));
    transactionPersistor = new GBTransactionPersistor(gbManager.getMap(TRANSACTION,
                                                                       GlobalTransactionID.class,
                                                                       GlobalTransactionDescriptor.class));
    persistentMapStore = new GBPersistentMapStore(gbManager.getMap(STATE_MAP, String.class, String.class));
    clientStatePersistor = new GBClientStatePersistor(sequenceManager.getSequence(CLIENT_STATE_SEQUENCE), gbManager.getMap(CLIENT_STATES, ChannelID.class, Boolean.class));
    managedObjectPersistor = new GBManagedObjectPersistor(gbManager.getMap(ROOT_DB, String.class, ObjectID.class), gbManager.getMap(OBJECT_DB, Long.class, byte[].class), sequenceManager.getSequence(OBJECT_ID_SEQUENCE), objectIDSetMaintainer);
    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);
    persistenceTransactionProvider = new GBPersistenceTransactionProvider(gbManager);
    persistentObjectFactory = new GBPersistentObjectFactory(gbManager, mapFactory);
  }

  private void verifyOrCreate(GBManager manager) {
    GBManagerConfiguration configuration = manager.getConfiguration();

    if (configuration.mapConfig().isEmpty()) {
      configuration.mapConfig().put(TRANSACTION, GBTransactionPersistor.config());
      configuration.mapConfig().put(CLIENT_STATES, GBClientStatePersistor.config());
      configuration.mapConfig().put(OBJECT_DB, GBManagedObjectPersistor.objectConfig(
              manager));
      configuration.mapConfig().put(ROOT_DB, GBManagedObjectPersistor.rootMapConfig());
      configuration.mapConfig().put(STATE_MAP, GBPersistentMapStore.config());
      configuration.mapConfig().put(SEQUENCE_MAP, GBSequence.config());
    }

    ((GBMapConfig<Long, byte[]>) configuration.mapConfig().get(OBJECT_DB)).addListener(objectIDSetMaintainer);
  }

  @Override
  public void close() {
    gbManager.shutdown();
  }

  @Override
  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return persistenceTransactionProvider;
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
