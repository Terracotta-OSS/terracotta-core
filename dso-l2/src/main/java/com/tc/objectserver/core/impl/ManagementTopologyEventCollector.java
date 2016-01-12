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
package com.tc.objectserver.core.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.object.EntityID;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.util.Assert;
import com.tc.util.State;


/**
 * The basic implementation of ITopologyEventCollector which ensures that events aren't obviously incorrect by ensuring
 * consistent state and symmetry of events.
 */
public class ManagementTopologyEventCollector implements ITopologyEventCollector {
  private final Set<ClientID> connectedClients;
  private final Set<EntityID> entities;
  private final Map<FetchTuple, Integer> fetchPairCounts;
  private boolean isActiveState;

  public ManagementTopologyEventCollector() {
    this.connectedClients = new HashSet<ClientID>();
    this.entities = new HashSet<EntityID>();
    this.fetchPairCounts = new HashMap<FetchTuple, Integer>();
    this.isActiveState = false;
  }

  @Override
  public synchronized void serverDidEnterState(State state) {
    // We track whether or not we are in an active state to ensure that entities are created/loaded in the expected state.
    this.isActiveState = (StateManager.ACTIVE_COORDINATOR == state);
  }

  @Override
  public synchronized void clientDidConnect(ClientID client) {
    // Ensure that this client isn't already connected.
    Assert.assertFalse(this.connectedClients.contains(client));
    // Now, add it to the connected set.
    this.connectedClients.add(client);
  }

  @Override
  public synchronized void clientDidDisconnect(ClientID client) {
    // Ensure that this client was already connected.
    Assert.assertTrue(this.connectedClients.contains(client));
    // Now, remove it from the connected set.
    this.connectedClients.remove(client);
  }

  @Override
  public synchronized void entityWasCreated(EntityID id, boolean isActive) {
    // Ensure that this is the expected state.
    Assert.assertTrue(isActive == this.isActiveState);
    // Ensure that this entity didn't already exist.
    Assert.assertFalse(this.entities.contains(id));
    // Now, add it to the set.
    this.entities.add(id);
  }

  @Override
  public synchronized void entityWasDestroyed(EntityID id) {
    // Ensure that this entity already exists.
    Assert.assertTrue(this.entities.contains(id));
    // Now, remove it from the set.
    this.entities.remove(id);
  }

  @Override
  public synchronized void entityWasReloaded(EntityID id, boolean isActive) {
    // Ensure that this is the expected state.
    // NOTE:  We currently can't verify the isActiveState since the reload path sets it _after_ the entities are reloaded.
    // Note that this could happen due to promotion or reloading from restart so we can't know if it already is in our set.
    if (!this.entities.contains(id)) {
      // Seems to be new so add it to the set.
      this.entities.add(id);
    }
  }

  @Override
  public synchronized void clientDidFetchEntity(ClientID client, EntityID id) {
    // XXX: Note that there is currently no handling of reconnect on entity promotion from passive to active so there may
    // be double-counting, here.
    FetchTuple tuple = new FetchTuple(client, id);
    int count = 0;
    if (this.fetchPairCounts.containsKey(tuple)) {
      count = this.fetchPairCounts.get(tuple);
    }
    count += 1;
    this.fetchPairCounts.put(tuple, count);
  }

  @Override
  public synchronized void clientDidReleaseEntity(ClientID client, EntityID id) {
    FetchTuple tuple = new FetchTuple(client, id);
    // We can't release something we didn't fetch.
    Assert.assertTrue(this.fetchPairCounts.containsKey(tuple));
    int count = this.fetchPairCounts.remove(tuple);
    count -= 1;
    if (count > 0) {
      this.fetchPairCounts.put(tuple, count);
    }
  }
  
  
  private static class FetchTuple {
    private final ClientID client;
    private final EntityID entity;
    
    public FetchTuple(ClientID client, EntityID entity) {
      this.client = client;
      this.entity = entity;
    }
    
    @Override
    public int hashCode() {
      return this.client.hashCode() ^ 
          (this.entity.hashCode() << 1);
    }
    
    @Override
    public boolean equals(Object obj) {
      boolean isEqual = false;
      if (obj instanceof FetchTuple) {
        FetchTuple other = (FetchTuple) obj;
        isEqual = this.client.equals(other.client)
            && this.entity.equals(other.entity);
      }
      return isEqual;
    }
  }
}
