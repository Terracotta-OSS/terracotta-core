package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;
import org.terracotta.corestorage.StorageManager;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author tim
 */
public class TransactionPersistorImpl implements TransactionPersistor {
  private static final String TRANSACTION = "transaction";

  private final KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public TransactionPersistorImpl(StorageManager storageManager) {
    this.committed = storageManager.getKeyValueStorage(TRANSACTION, GlobalTransactionID.class, GlobalTransactionDescriptor.class);
  }

  public static void addConfigsTo(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
    configMap.put(TRANSACTION, ImmutableKeyValueStorageConfig.builder(GlobalTransactionID.class, GlobalTransactionDescriptor.class)
        .valueTransformer(GlobalTransactionDescriptorSerializer.INSTANCE)
        .keyTransformer(GlobalTransactionIDSerializer.INSTANCE).build());
  }

  @Override
  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return committed.values();
  }

  @Override
  public void saveGlobalTransactionDescriptor(GlobalTransactionDescriptor gtx) {
    committed.put(gtx.getGlobalTransactionID(), gtx);
  }

  @Override
  public void deleteAllGlobalTransactionDescriptors(SortedSet<GlobalTransactionID> globalTransactionIDs) {
    committed.removeAll(globalTransactionIDs);
  }

  private static class GlobalTransactionIDSerializer extends AbstractIdentifierTransformer<GlobalTransactionID> {
    static final GlobalTransactionIDSerializer INSTANCE = new GlobalTransactionIDSerializer();

    GlobalTransactionIDSerializer() {
      super(GlobalTransactionID.class);
    }

    @Override
    protected GlobalTransactionID createIdentifier(final long id) {
      return new GlobalTransactionID(id);
    }
  }

  private static class GlobalTransactionDescriptorSerializer extends Serializer<GlobalTransactionDescriptor> {
    static final GlobalTransactionDescriptorSerializer INSTANCE = new GlobalTransactionDescriptorSerializer();

    @Override
    public GlobalTransactionDescriptor recover(final ByteBuffer buffer) {
      GlobalTransactionID gid = new GlobalTransactionID(buffer.getLong());
      ServerTransactionID sid = new ServerTransactionID(new ClientID(buffer.getLong()), new TransactionID(buffer.getLong()));
      return new GlobalTransactionDescriptor(sid, gid);
    }

    @Override
    public ByteBuffer transform(final GlobalTransactionDescriptor globalTransactionDescriptor) {
      ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE * 3);
      buffer.putLong(globalTransactionDescriptor.getGlobalTransactionID().toLong());
      buffer.putLong(((ClientID) globalTransactionDescriptor.getServerTransactionID().getSourceID()).toLong());
      buffer.putLong(globalTransactionDescriptor.getClientTransactionID().toLong());
      buffer.flip();
      return buffer;
    }

    @Override
    public boolean equals(final GlobalTransactionDescriptor left, final ByteBuffer right) {
      return left.equals(recover(right));
    }
  }
}
