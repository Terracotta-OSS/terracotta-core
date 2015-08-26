package com.tc.objectserver.persistence;


import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;

/**
 * @author tim
 */
public class ClientStatePersistor {
  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String CLIENT_STATES =  "client_states";

  private final MutableSequence clientIDSequence;
  private final KeyValueStorage<ChannelID, Boolean> clients;

  public ClientStatePersistor(SequenceManager sequenceManager, IPersistentStorage storageManager) {
    this.clientIDSequence = sequenceManager.getSequence(CLIENT_STATE_SEQUENCE);
    this.clients = storageManager.getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class);
  }

  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  public Set<ChannelID> loadClientIDs() {
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
}