package com.tc.objectserver.persistence.gb;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.inmemory.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;

import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.heap.KeyValueStorageConfigImpl;

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
    return new KeyValueStorageConfigImpl<ChannelID, Boolean>(ChannelID.class, Boolean.class);
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
}
