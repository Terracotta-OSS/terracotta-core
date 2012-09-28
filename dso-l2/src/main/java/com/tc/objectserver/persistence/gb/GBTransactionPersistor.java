package com.tc.objectserver.persistence.gb;

import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBSerializer;
import com.tc.gbapi.impl.GBOnHeapMapConfig;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.util.Collection;
import java.util.SortedSet;

/**
 * @author tim
 */
public class GBTransactionPersistor implements TransactionPersistor {
  private final GBMap<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public GBTransactionPersistor(GBMap<GlobalTransactionID, GlobalTransactionDescriptor> committed) {
    this.committed = committed;
  }

  public static GBMapConfig<GlobalTransactionID, GlobalTransactionDescriptor> config() {
    return new GBOnHeapMapConfig<GlobalTransactionID, GlobalTransactionDescriptor>(GlobalTransactionID.class, GlobalTransactionDescriptor.class);
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
