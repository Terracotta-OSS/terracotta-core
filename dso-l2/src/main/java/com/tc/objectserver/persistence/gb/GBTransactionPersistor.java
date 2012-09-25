package com.tc.objectserver.persistence.gb;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.gb.gbapi.GBManager;
import com.tc.objectserver.persistence.gb.gbapi.GBMap;
import com.tc.objectserver.persistence.gb.gbapi.GBMapConfig;
import com.tc.objectserver.persistence.gb.gbapi.GBMapMutationListener;
import com.tc.objectserver.persistence.gb.gbapi.GBSerializer;
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
