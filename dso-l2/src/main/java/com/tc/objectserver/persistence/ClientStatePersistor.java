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
import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import org.terracotta.persistence.IPlatformPersistence;


public class ClientStatePersistor {
  private static final String UUID_FILE_NAME =  "uuid.dat";
  private static final String CLIENTS_MAP_FILE_NAME =  "clients_map.map";
  private static final String NEXT_CLIENT_ID_FILE_NAME =  "next_client_id.dat";
  
  
  private final IPlatformPersistence storageManager;
  private final String serverUUID;
  private final HashMap<ChannelID, Boolean> clients;
  private final MutableSequence clientIDSequence;

  @SuppressWarnings("unchecked")
  public ClientStatePersistor(IPlatformPersistence storageManager) {
    this.storageManager = storageManager;
    
    String uuid = null;
    HashMap<ChannelID, Boolean> clients = null;
    try {
      uuid = (String) this.storageManager.loadDataElement(UUID_FILE_NAME);
      if (null == uuid) {
        // Set the default.
        uuid = UUID.getUUID().toString();
        this.storageManager.storeDataElement(UUID_FILE_NAME, uuid);
      }
      clients = (HashMap<ChannelID, Boolean>) this.storageManager.loadDataElement(CLIENTS_MAP_FILE_NAME);
      if (null == clients) {
        clients = new HashMap<>();
      }
    } catch (IOException e) {
      // We don't expect this during startup so just throw it as runtime.
      throw new RuntimeException("Failure reading ClientStatePersistor data", e);
    }
    this.serverUUID = uuid;
    this.clients = clients;
    this.clientIDSequence = new Sequence(this.storageManager);
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
    safeStoreClients();
  }

  public void deleteClientState(ChannelID id) throws ClientNotFoundException {
    if (!clients.remove(id)) {
      throw new ClientNotFoundException();
    }
    safeStoreClients();
  }

  public String getServerUUID() {
    return this.serverUUID;
  }

  private void safeStoreClients() {
    try {
      this.storageManager.storeDataElement(CLIENTS_MAP_FILE_NAME, this.clients);
    } catch (IOException e) {
      // Not expected during run.
      Assert.fail(e.getLocalizedMessage());
    }
  }


  private static class Sequence implements MutableSequence {
    private final IPlatformPersistence storageManager;
    private long next;

    Sequence(IPlatformPersistence storageManager) {
      this.storageManager = storageManager;
      long nextID = 0;
      try {
        Long nextInStorage = (Long) this.storageManager.loadDataElement(NEXT_CLIENT_ID_FILE_NAME);
        if (null != nextInStorage) {
          nextID = nextInStorage;
        }
      } catch (IOException e) {
        // We don't expect this during startup so just throw it as runtime.
        throw new RuntimeException("Failure reading ClientStatePersistor next client ID", e);
      }
      this.next = nextID;
    }

    @Override
    public synchronized void setNext(long next) {
      if (next < this.next) {
        throw new AssertionError("next=" + next + " current=" + this.next);
      }
      this.next = next;
      storeNextID();
    }

    @Override
    public synchronized long next() {
      long r = next;
      next += 1;
      storeNextID();
      return r;
    }

    @Override
    public synchronized long current() {
      return next;
    }
    
    private void storeNextID() {
      try {
        this.storageManager.storeDataElement(NEXT_CLIENT_ID_FILE_NAME, this.next);
      } catch (IOException e) {
        // We don't expect this during startup so just throw it as runtime.
        throw new RuntimeException("Failure storing ClientStatePersistor next client ID", e);
      }
    }
  }
}
