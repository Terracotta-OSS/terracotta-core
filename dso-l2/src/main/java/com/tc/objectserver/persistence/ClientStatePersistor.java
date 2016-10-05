/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.persistence;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.util.Set;
import org.terracotta.persistence.IPersistentStorage;
import org.terracotta.persistence.KeyValueStorage;


public class ClientStatePersistor {
  private static final String CLIENT_STATES =  "client_states";
  private static final String UUID_CONTAINER =  "uuid_container";
  private static final String UUID_KEY =  "uuid_container:key";
  private static final String SEQUENCE_CONTAINER = "sequence_container";
  private static final String SEQUENCE_KEY =  "sequence_container:key";

  private final KeyValueStorage<String, String> uuidContainer;
  private final KeyValueStorage<ChannelID, Boolean> clients;
  private final MutableSequence clientIDSequence;

  public ClientStatePersistor(IPersistentStorage storageManager) {
    this.uuidContainer = storageManager.getKeyValueStorage(UUID_CONTAINER, String.class, String.class);
    if (!this.uuidContainer.containsKey(UUID_KEY)) {
      this.uuidContainer.put(UUID_KEY, UUID.getUUID().toString());
    }
    this.clients = storageManager.getKeyValueStorage(CLIENT_STATES, ChannelID.class, Boolean.class);
    
    KeyValueStorage<String, Long> sequenceContainer = storageManager.getKeyValueStorage(SEQUENCE_CONTAINER, String.class, Long.class);
    this.clientIDSequence = new Sequence(sequenceContainer);
  }

  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  @SuppressWarnings("deprecation")
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

  public String getServerUUID() {
    return this.uuidContainer.get(UUID_KEY);
  }


  private static class Sequence implements MutableSequence {
    private final KeyValueStorage<String, Long> sequenceContainer;
    private long next;

    Sequence(KeyValueStorage<String, Long> sequenceContainer) {
      this.sequenceContainer = sequenceContainer;
      if (!this.sequenceContainer.containsKey(SEQUENCE_KEY)) {
        this.sequenceContainer.put(SEQUENCE_KEY, 0L);
      }
      this.next = this.sequenceContainer.get(SEQUENCE_KEY);
    }

    @Override
    public synchronized void setNext(long next) {
      if (next < this.next) {
        throw new AssertionError("next=" + next + " current=" + this.next);
      }
      this.next = next;
      this.sequenceContainer.put(SEQUENCE_KEY, next);
    }

    @Override
    public synchronized long next() {
      long r = next;
      next += 1;
      this.sequenceContainer.put(SEQUENCE_KEY, next);
      return r;
    }

    @Override
    public synchronized long current() {
      return next;
    }
  }
}
