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

import com.tc.net.ClientID;
import com.tc.objectserver.api.ClientNotFoundException;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.sequence.MutableSequence;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.terracotta.persistence.IPlatformPersistence;


public class ClientStatePersistor implements PrettyPrintable {
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
    this.clientIDSequence = new Sequence(this.storageManager);
    Assert.assertNotNull(this.clients);
  }

  public MutableSequence getConnectionIDSequence() {
    return clientIDSequence;
  }

  public Set<ClientID> loadClientIDs() {
    return clients.keySet();
  }

  public boolean containsClient(ClientID id) {
    return clients.containsKey(id);
  }

  public boolean saveClientState(ClientID channelID) {
    if (clients.put(channelID, true) == null) {
      safeStoreClients();
      return true;
    } else {
      return false;
    }
  }

  public void deleteClientState(ClientID id) throws ClientNotFoundException {
    if (clients.remove(id) == null) {
      throw new ClientNotFoundException();
    }
    safeStoreClients();
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.indent().println(this.getClass().getName());

    out.indent().indent().println("Connected clients: ");
    for (ClientID clientID : clients.keySet()) {
      out.indent().indent().indent().println(clientID);
    }
    out.indent().indent().println("Next client id: " + clientIDSequence.current());

    return out;
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
