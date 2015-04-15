/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;
import org.terracotta.corestorage.monitoring.MonitoredResource;

import com.tc.properties.TCPropertiesConsts;
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

  private final PersistentObjectFactory persistentObjectFactory;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final ClusterStatePersistor clusterStatePersistor;

  private TransactionPersistor transactionPersistor;
  private ManagedObjectPersistor managedObjectPersistor;
  private MutableSequence gidSequence;
  private ClientStatePersistor clientStatePersistor;
  private SequenceManager sequenceManager;
  private InlineGCPersistor inlineGCPersistor;
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
    clusterStatePersistor = new ClusterStatePersistor(storageManager);
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
    inlineGCPersistor = createInlineGCPersistor(storageManager);

    started = true;
  }

  protected TransactionPersistor createTransactionPersistor(StorageManager storageManagerParam) {
    return new NullTransactionPersistor();
  }

  protected InlineGCPersistor createInlineGCPersistor(StorageManager storageMgr) {
    return new HeapInlineGCPersistor();
  }

  public void close() {
    storageManager.close();
  }
  
  public Collection<MonitoredResource> getMonitoredResources() {
    checkStarted();
    return storageManager.getMonitoredResources();
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

  public ClusterStatePersistor getClusterStatePersistor() {
    return clusterStatePersistor;
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

  public InlineGCPersistor getInlineGCPersistor() {
    checkStarted();
    return inlineGCPersistor;
  }

  @Override
  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(getClass().getName()).flush();
    if (!started) {
      out.indent().print("PersistorImpl not started.").flush();
    } else {
      Collection<MonitoredResource> list = storageManager.getMonitoredResources();
      for ( MonitoredResource rsrc : list ) {
          out.indent().print("Resource Type: " + rsrc.getType()).flush();
          out.indent().print("Resource Total: " + safeByteSizeAsString(rsrc.getTotal())).flush();
          out.indent().print("Resource Reserved: " + safeByteSizeAsString(rsrc.getReserved())).flush();
          out.indent().print("Resource Used: " + safeByteSizeAsString(rsrc.getUsed())).flush();
      }
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
