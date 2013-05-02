package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.object.persistence.api.PersistentMapStore;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Conversion;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tim
 */
public class Persistor implements PrettyPrintable {
  private static final String GLOBAL_TRANSACTION_ID_SEQUENCE = "global_transaction_id_sequence";

  private final StorageManager storageManager;

  private volatile boolean started = false;

  private final PersistentMapStore persistentMapStore;
  private final PersistentObjectFactory persistentObjectFactory;
  private final PersistenceTransactionProvider persistenceTransactionProvider;

  private TransactionPersistor transactionPersistor;
  private ManagedObjectPersistor managedObjectPersistor;
  private MutableSequence gidSequence;
  private ClientStatePersistor clientStatePersistor;
  private SequenceManager sequenceManager;
  private final ObjectIDSetMaintainer objectIDSetMaintainer;

  private EvictionTransactionPersistor evictionTransactionPersistor;

  public Persistor(StorageManagerFactory storageManagerFactory) {
    objectIDSetMaintainer = new ObjectIDSetMaintainer();
    try {
      storageManager = storageManagerFactory
          .createStorageManager(getDataStorageConfigs(storageManagerFactory),
          new SingletonTransformerLookup(Object.class, LiteralSerializer.INSTANCE));
    } catch (IOException e) {
      throw new AssertionError(e);
    }

    persistenceTransactionProvider = new PersistenceTransactionProvider(storageManager);
    persistentObjectFactory = new PersistentObjectFactory(storageManager, storageManagerFactory);
    persistentMapStore = new PersistentMapStoreImpl(storageManager);
  }

  public StorageManager getStorageManager() {
    return storageManager;
  }

  private Map<String, KeyValueStorageConfig<?, ?>> getDataStorageConfigs(StorageManagerFactory storageManagerFactory) {
    Map<String, KeyValueStorageConfig<?, ?>> configs = new HashMap<String, KeyValueStorageConfig<?, ?>>();
    ClientStatePersistor.addConfigsTo(configs);
    ManagedObjectPersistor.addConfigsTo(configs, objectIDSetMaintainer, storageManagerFactory);
    SequenceManager.addConfigsTo(configs);
    addAdditionalConfigs(configs, storageManagerFactory);
    return configs;
  }

  protected void addAdditionalConfigs(Map<String, KeyValueStorageConfig<?, ?>> configMap, StorageManagerFactory storageManagerFactory) {
    // override in a subclass
  }

  public void start() {
    try {
      storageManager.start().get();
    } catch (Exception e) {
      throw new AssertionError(e);
    }

    sequenceManager = new SequenceManager(storageManager);
    transactionPersistor = createTransactionPersistor(storageManager);
    clientStatePersistor = new ClientStatePersistor(sequenceManager, storageManager);
    managedObjectPersistor = new ManagedObjectPersistor(storageManager, sequenceManager, objectIDSetMaintainer);

    gidSequence = sequenceManager.getSequence(GLOBAL_TRANSACTION_ID_SEQUENCE);
    evictionTransactionPersistor = createEvictionTransactionPersistor(storageManager);

    started = true;
  }

  protected TransactionPersistor createTransactionPersistor(StorageManager storageManagerParam) {
    return new NullTransactionPersistor();
  }

  public void close() {
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

  protected final void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Persistor is not yet started.");
    }
  }

  protected EvictionTransactionPersistor createEvictionTransactionPersistor(StorageManager storageMgr) {
    return new NullEvictionTransactionPersistorImpl();
  }

  public EvictionTransactionPersistor getEvictionTransactionPersistor() {
    checkStarted();
    return this.evictionTransactionPersistor;
  }

  @Override
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(getClass().getName()).flush();
    if (!started) {
      out.indent().print("PersistorImpl not started.").flush();
    } else {
      out.indent().print("Resource Type: " + getMonitoredResource().getType()).flush();
      out.indent().print("Resource Total: " + safeByteSizeAsString(getMonitoredResource().getTotal())).flush();
      out.indent().print("Resource Reserved: " + safeByteSizeAsString(getMonitoredResource().getReserved())).flush();
      out.indent().print("Resource Used: " + safeByteSizeAsString(getMonitoredResource().getUsed())).flush();
    }
    return out;
  }

  private static String safeByteSizeAsString(long size) {
    try {
      return Conversion.memoryBytesAsSize(size);
    } catch (Conversion.MetricsFormatException e) {
      return size + "b";
    }
  }
}
