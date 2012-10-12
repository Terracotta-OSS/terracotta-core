package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.SortedSet;

/**
 * @author tim
 */
public class TransactionPersistor {
  private final KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed;

  public TransactionPersistor(KeyValueStorage<GlobalTransactionID, GlobalTransactionDescriptor> committed) {
    this.committed = committed;
  }

  public static KeyValueStorageConfig<GlobalTransactionID, GlobalTransactionDescriptor> config() {
    KeyValueStorageConfig<GlobalTransactionID, GlobalTransactionDescriptor> config = new KeyValueStorageConfigImpl<GlobalTransactionID, GlobalTransactionDescriptor>(GlobalTransactionID.class, GlobalTransactionDescriptor.class);
    config.setKeySerializer(GlobalTransactionIDSerializer.INSTANCE);
    config.setValueSerializer(GlobalTransactionDescriptorSerializer.INSTANCE);
    return config;
  }

  public Collection<GlobalTransactionDescriptor> loadAllGlobalTransactionDescriptors() {
    return committed.values();
  }

  public void saveGlobalTransactionDescriptor(Transaction tx, GlobalTransactionDescriptor gtx) {
    committed.put(gtx.getGlobalTransactionID(), new GlobalTransactionDescriptor(gtx.getServerTransactionID(), gtx.getGlobalTransactionID()));
  }

  public void deleteAllGlobalTransactionDescriptors(Transaction tx, SortedSet<GlobalTransactionID> globalTransactionIDs) {
    committed.removeAll(globalTransactionIDs);
  }

  private static class GlobalTransactionIDSerializer extends AbstractIdentifierSerializer<GlobalTransactionID> {
    static final GlobalTransactionIDSerializer INSTANCE = new GlobalTransactionIDSerializer();

    GlobalTransactionIDSerializer() {
      super(GlobalTransactionID.class);
    }

    @Override
    protected GlobalTransactionID createIdentifier(final long id) {
      return new GlobalTransactionID(id);
    }
  }

  private static class GlobalTransactionDescriptorSerializer implements Serializer<GlobalTransactionDescriptor> {
    static final GlobalTransactionDescriptorSerializer INSTANCE = new GlobalTransactionDescriptorSerializer();

    @Override
    public GlobalTransactionDescriptor deserialize(final ByteBuffer buffer) {
      GlobalTransactionID gid = new GlobalTransactionID(buffer.getLong());
      ServerTransactionID sid = new ServerTransactionID(new ClientID(buffer.getLong()), new TransactionID(buffer.getLong()));
      return new GlobalTransactionDescriptor(sid, gid);
    }

    @Override
    public ByteBuffer serialize(final GlobalTransactionDescriptor globalTransactionDescriptor) {
      ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE * 3);
      buffer.putLong(globalTransactionDescriptor.getGlobalTransactionID().toLong());
      buffer.putLong(((ClientID) globalTransactionDescriptor.getServerTransactionID().getSourceID()).toLong());
      buffer.putLong(globalTransactionDescriptor.getClientTransactionID().toLong());
      buffer.flip();
      return buffer;
    }

    @Override
    public boolean equals(final ByteBuffer left, final Object right) {
      if (right instanceof GlobalTransactionDescriptor) {
        return deserialize(left).equals(right);
      } else {
        return false;
      }
    }
  }
}
