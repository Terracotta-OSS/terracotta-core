package com.tc.objectserver.persistence.gb;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.gb.gbapi.GBMap;
import com.tc.objectserver.persistence.gb.gbapi.GBMapConfig;
import com.tc.objectserver.persistence.gb.gbapi.GBMapMutationListener;
import com.tc.objectserver.persistence.gb.gbapi.GBSerializer;
import com.tc.objectserver.persistence.inmemory.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;

/**
 * @author tim
 */
public class GBClientStatePersistor implements ClientStatePersistor {
  private final GBSequence clientIDSequence;
  private final GBMap<ChannelID, Boolean> clients;

  public GBClientStatePersistor(GBSequence clientIDSequence, GBMap<ChannelID, Boolean> clients) {
    this.clientIDSequence = clientIDSequence;
    this.clients = clients;
  }

  public static GBMapConfig<ChannelID, Boolean> config() {
    return new GBMapConfig<ChannelID, Boolean>() {
      @Override
      public void setKeySerializer(GBSerializer<ChannelID> serializer) {
      }

      @Override
      public void setValueSerializer(GBSerializer<Boolean> serializer) {
      }

      @Override
      public Class<ChannelID> getKeyClass() {
        return null;
      }

      @Override
      public Class<Boolean> getValueClass() {
        return null;
      }

      @Override
      public void addListener(GBMapMutationListener<ChannelID, Boolean> listener) {
      }
    };
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
