/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.persistence;

import com.tc.net.ClientID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.util.Assert;
import com.tc.net.core.ProductID;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.terracotta.persistence.IPlatformPersistence;


public class ClientStatePersistor {
  private static final String CLIENTS_MAP_FILE_NAME =  "clients_map.map";
  private static final String NEXT_CLIENT_ID_FILE_NAME =  "next_client_id.dat";
  
  
  private final IPlatformPersistence storageManager;
  private final ConcurrentHashMap<ClientID, Boolean> clients;
  private final MutableSequence clientIDSequence;
  
  @SuppressWarnings("unchecked")
  public ClientStatePersistor(IPlatformPersistence storageManager) {
    this.storageManager = storageManager;
    
    ConcurrentHashMap<ClientID, Boolean> clientsMap = null;
    try {
      clientsMap = (ConcurrentHashMap<ClientID, Boolean>) this.storageManager.loadDataElement(CLIENTS_MAP_FILE_NAME);
      if (null == clientsMap) {
        clientsMap = new ConcurrentHashMap<>();
      }
    } catch (IOException e) {
      // We don't expect this during startup so just throw it as runtime.
      throw new RuntimeException("Failure reading ClientStatePersistor data", e);
    }
    this.clients = clientsMap;
    // orphaned clients are not supposed to reconnect on restart.  since we are repopulating 
    // restart data, set aside the orphaned clients
    this.clientIDSequence = new Sequence(this.storageManager);
    Assert.assertNotNull(this.clients);
  }

  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  public Set<ClientID> loadOrphanClientIDs() {
    return clients.entrySet().stream().filter(entry->!entry.getValue()).map((entry)->entry.getKey()).collect(Collectors.toSet());
  }
  
  public Set<ClientID> loadPermanentClientIDs() {
    return clients.entrySet().stream().filter(entry->entry.getValue()).map((entry)->entry.getKey()).collect(Collectors.toSet());
  }
  
  public Set<ClientID> loadAllClientIDs() {
    return clients.entrySet().stream().map(entry->entry.getKey()).collect(Collectors.toSet());
  }

  public boolean containsClient(ClientID id) {
    return clients.containsKey(id);
  }

  public void saveClientState(ClientID channelID, ProductID product) {
    // if the client is in the orphaned set, do not add it to the saved list because 
    // it should never connect again.  this can happen if the ConnectionIDFactory services
    // a connection before the existing clients are loaded into the reconnect window
    clients.put(channelID, product.isPermanent());
    safeStoreClients();
  }

  public void deleteClientState(ClientID id) throws ClientNotFoundException {
    if (clients.remove(id) == null) {
      throw new ClientNotFoundException();
    }
    safeStoreClients();
  }

  Map<String, Object> reportStateToMap(Map<String, Object> map) {
    map.put("className", this.getClass().getName());
    List<String> cs = new ArrayList<>();
    map.put("clients", cs);
    for (ClientID clientID : clients.keySet()) {
      cs.add(clientID.toString());
    }
    map.put("next", clientIDSequence.current());

    return map;
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
