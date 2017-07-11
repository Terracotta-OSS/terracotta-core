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
import com.tc.util.ProductID;
import java.util.LinkedHashMap;
import java.util.Map;

import org.terracotta.persistence.IPlatformPersistence;


public class Persistor implements PrettyPrintable {
  private final IPlatformPersistence persistentStorage;
  private boolean wasDBClean;

  private volatile boolean started = false;

  private final ClusterStatePersistor clusterStatePersistor;

  private ClientStatePersistor clientStatePersistor;
  private final EntityPersistor entityPersistor;
  private TransactionOrderPersistor transactionOrderPersistor;

  public Persistor(IPlatformPersistence persistentStorage) {
    this.persistentStorage = persistentStorage;
    this.clusterStatePersistor = new ClusterStatePersistor(persistentStorage);
    this.entityPersistor = new EntityPersistor(persistentStorage);
  }

  public boolean start(boolean trackClients) {
    clientStatePersistor = new ClientStatePersistor(persistentStorage);
    this.transactionOrderPersistor = new TransactionOrderPersistor(persistentStorage, this.clientStatePersistor.loadClientIDs());
    for (ClientID orphan : clientStatePersistor.loadOrphanClientIDs()) {
      try {
        removeClientState(orphan);
      } catch (ClientNotFoundException notfound) {
        // do nothing
      }
    }
    //  remove any entries in the entity journal, clients aren't tracked
    if (!trackClients) {
      entityPersistor.clearEntityClientJournal();
    }
    wasDBClean = this.clusterStatePersistor.isDBClean();
    started = true;
    return wasDBClean;
  }

  public void close() {
  }
  
  public void addClientState(ClientID node, ProductID product) {
    clientStatePersistor.saveClientState(node, product);
    entityPersistor.addTrackingForClient(node);
    transactionOrderPersistor.addTrackingForClient(node, product);
  }
  
  public void removeClientState(ClientID node) throws ClientNotFoundException {
    //  removing the client state.  threading doesn't matter here.  A client that is gone will never come back
    //  code the underlying defensively to handle the fact that the client is gone
    transactionOrderPersistor.removeTrackingForClient(node);
    entityPersistor.removeTrackingForClient(node);
    clientStatePersistor.deleteClientState(node);
  }
  
  public ClientStatePersistor getClientStatePersistor() {
    checkStarted();
    return clientStatePersistor;
  }

  public ClusterStatePersistor getClusterStatePersistor() {
    return clusterStatePersistor;
  }

  public EntityPersistor getEntityPersistor() {
    return this.entityPersistor;
  }

  public TransactionOrderPersistor getTransactionOrderPersistor() {
    return this.transactionOrderPersistor;
  }

  protected final void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Persistor is not yet started.");
    }
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    Map<String,Object> map = new LinkedHashMap<>();
    map.put("className", this.getClass().getName());
    map.put("started", started);

    if(clusterStatePersistor != null) {
      Map<String,Object> substate = new LinkedHashMap<>();
      map.put("clusterState", substate);
      clusterStatePersistor.reportStateToMap(substate);
    }
    if(entityPersistor != null) {
      Map<String,Object> substate = new LinkedHashMap<>();
      map.put("entityPersistor", substate);
      entityPersistor.reportStateToMap(substate);
    }
    if(clientStatePersistor != null) {
      Map<String,Object> substate = new LinkedHashMap<>();
      map.put("clientPersistor", substate);
      clientStatePersistor.reportStateToMap(substate);
    }
    if(transactionOrderPersistor != null) {
      Map<String,Object> substate = new LinkedHashMap<>();
      map.put("orderPersistor", substate);
      transactionOrderPersistor.reportStateToMap(substate);
    }

    out.println(map);
    return out;
  }
}
