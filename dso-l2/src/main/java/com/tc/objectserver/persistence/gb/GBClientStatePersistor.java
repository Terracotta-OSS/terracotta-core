package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.inmemory.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * @author tim
 */
public class GBClientStatePersistor implements ClientStatePersistor {
  private final GBSequence clientIDSequence;
  private final KeyValueStorage<ChannelID, Boolean> clients;

  public GBClientStatePersistor(GBSequence clientIDSequence, KeyValueStorage<ChannelID, Boolean> clients) {
    this.clientIDSequence = clientIDSequence;
    this.clients = clients;
  }

  public static KeyValueStorageConfig<ChannelID, Boolean> config() {
    KeyValueStorageConfig<ChannelID, Boolean> config = new KeyValueStorageConfigImpl<ChannelID, Boolean>(ChannelID.class, Boolean.class);
    config.setKeySerializer(ChannelIDSerializer.INSTANCE);
    config.setValueSerializer(BooleanSerializer.INSTANCE);
    return config;
  }

  @Override
  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  @Override
  public Set loadClientIDs() {
    return clients.keySet();
  }

  @Override
  public boolean containsClient(ChannelID id) {
    return clients.containsKey(id);
  }

  @Override
  public void saveClientState(ChannelID channelID) {
    clients.put(channelID, true);
  }

  @Override
  public void deleteClientState(ChannelID id) throws ClientNotFoundException {
    if (!clients.remove(id)) {
      throw new ClientNotFoundException();
    }
  }

  private static class ChannelIDSerializer extends AbstractIdentifierSerializer<ChannelID> {
    static final ChannelIDSerializer INSTANCE = new ChannelIDSerializer();

    private ChannelIDSerializer() {
      super(ChannelID.class);
    }

    @Override
    protected ChannelID createIdentifier(final long id) {
      return new ChannelID(id);
    }
  }

  private static class BooleanSerializer implements Serializer<Boolean> {
    static final BooleanSerializer INSTANCE = new BooleanSerializer();

    @Override
    public Boolean deserialize(final ByteBuffer buffer) {
      if (buffer.get() == 0) {
        return Boolean.FALSE;
      } else {
        return Boolean.TRUE;
      }
    }

    @Override
    public ByteBuffer serialize(final Boolean aBoolean) {
      byte[] b = new byte[1];
      if (aBoolean) {
        b[0] = 1;
      } else {
        b[0] = 0;
      }
      return ByteBuffer.wrap(b);
    }

    @Override
    public boolean equals(final ByteBuffer left, final Object right) {
      if (right instanceof Boolean) {
        return deserialize(left).equals(right);
      } else {
        return false;
      }
    }
  }
}
