package com.tc.objectserver.persistence.gb;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.gb.gbapi.GBMap;
import com.tc.objectserver.persistence.inmemory.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;

/**
 * @author tim
 */
public class GBClientStatePersistor implements ClientStatePersistor {
  private final GBSequence clientIDSequence = new GBSequence(null, null);
  private final GBMap<ChannelID, Boolean> clients = null;

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
