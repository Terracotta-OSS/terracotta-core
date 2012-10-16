package com.tc.objectserver.persistence;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.Serializer;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;
import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.Transformer;

/**
 * @author tim
 */
public class ClientStatePersistor {
  private final MutableSequence clientIDSequence;
  private final KeyValueStorage<ChannelID, Boolean> clients;

  public ClientStatePersistor(MutableSequence clientIDSequence, KeyValueStorage<ChannelID, Boolean> clients) {
    this.clientIDSequence = clientIDSequence;
    this.clients = clients;
  }

  public static KeyValueStorageConfig<ChannelID, Boolean> config() {
    return new ImmutableKeyValueStorageConfig<ChannelID, Boolean>(ChannelID.class, Boolean.class, ChannelIDTransformer.INSTANCE, (Transformer<Boolean, ?>) null);
  }

  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  public Set loadClientIDs() {
    return clients.keySet();
  }

  public boolean containsClient(ChannelID id) {
    return clients.containsKey(id);
  }

  public void saveClientState(ChannelID channelID) {
    clients.put(channelID, true);
  }

  public void deleteClientState(ChannelID id) throws ClientNotFoundException {
    if (!clients.remove(id)) {
      throw new ClientNotFoundException();
    }
  }

  private static class ChannelIDTransformer extends AbstractIdentifierTransformer<ChannelID> {
    static final ChannelIDTransformer INSTANCE = new ChannelIDTransformer();

    private ChannelIDTransformer() {
      super(ChannelID.class);
    }

    @Override
    protected ChannelID createIdentifier(final long id) {
      return new ChannelID(id);
    }
  }
}
