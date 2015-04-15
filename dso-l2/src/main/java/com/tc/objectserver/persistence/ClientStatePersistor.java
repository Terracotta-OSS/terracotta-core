/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.persistence;

import org.terracotta.corestorage.ImmutableKeyValueStorageConfig;
import org.terracotta.corestorage.KeyValueStorage;
import org.terracotta.corestorage.KeyValueStorageConfig;
import org.terracotta.corestorage.StorageManager;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.util.sequence.MutableSequence;

import java.util.Map;
import java.util.Set;

/**
 * @author tim
 */
public class ClientStatePersistor {
  private static final String CLIENT_STATE_SEQUENCE = "client_state_sequence";
  private static final String CLIENT_STATES = "client_states";

  private final MutableSequence clientIDSequence;
  private final KeyValueStorage<ChannelID, Boolean> clients;

  public ClientStatePersistor(SequenceManager sequenceManager, StorageManager storageManager) {
    this.clientIDSequence = sequenceManager.getSequence(CLIENT_STATE_SEQUENCE);
    this.clients = storageManager.getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class);
  }

  public static void addConfigsTo(Map<String, KeyValueStorageConfig<?, ?>> configMap) {
    configMap.put(CLIENT_STATES, ImmutableKeyValueStorageConfig.builder(ChannelID.class, Boolean.class).keyTransformer(ChannelIDTransformer.INSTANCE).build());
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
