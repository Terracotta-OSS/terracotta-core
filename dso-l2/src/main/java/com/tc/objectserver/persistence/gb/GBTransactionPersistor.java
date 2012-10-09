package com.tc.objectserver.persistence.gb;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.util.Collection;
import java.util.SortedSet;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

/**
 * @author tim
 */
public class GBTransactionPersistor implements TransactionPersistor {
  private final KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public GBTransactionPersistor(KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed) {
    this.committed = committed;
  }

  public static KeyValueStorageConfig<GlobalTransactionID, GlobalTransactionDescriptor> config() {
    return new KeyValueStorageConfigImpl<GlobalTransactionID, GlobalTransactionDescriptor>(GlobalTransactionID.class, GlobalTransactionDescriptor.class);
  }

  @Override
  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return committed.values();
  }

  @Override
  public void saveGlobalTransactionDescriptor(PersistenceTransaction tx, GlobalTransactionDescriptor gtx) {
    committed.put(gtx.getGlobalTransactionID(), gtx);
  }

  @Override
  public void deleteAllGlobalTransactionDescriptors(PersistenceTransaction tx, SortedSet<GlobalTransactionID> globalTransactionIDs) {
    committed.removeAll(globalTransactionIDs);
  }
}
