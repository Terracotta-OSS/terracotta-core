package com.tc.objectserver.persistence.gb;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.gbapi.GBManager;
import com.tc.gbapi.GBMap;
import com.tc.gbapi.GBMapConfig;
import com.tc.gbapi.GBMapMutationListener;
import com.tc.gbapi.GBSerializer;
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
    return new GBMapConfig<GlobalTransactionID, GlobalTransactionDescriptor>() {
      @Override
      public void setKeySerializer(GBSerializer<GlobalTransactionID> serializer) {
      }

      @Override
      public void setValueSerializer(GBSerializer<GlobalTransactionDescriptor> serializer) {
      }

      @Override
      public Class<GlobalTransactionID> getKeyClass() {
        return null;
      }

      @Override
      public Class<GlobalTransactionDescriptor> getValueClass() {
        return null;
      }

      @Override
      public void addListener(GBMapMutationListener<GlobalTransactionID, GlobalTransactionDescriptor> listener) {
      }
    };
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
