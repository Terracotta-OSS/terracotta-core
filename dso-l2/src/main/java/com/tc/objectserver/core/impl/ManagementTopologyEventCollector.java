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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformMonitoringConstants;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.entity.ClientDescriptorImpl;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.util.Assert;
import com.tc.util.State;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.terracotta.monitoring.ServerState;


/**
 * The basic implementation of ITopologyEventCollector which ensures that events aren't obviously incorrect by ensuring
 * consistent state and symmetry of events.
 */
public class ManagementTopologyEventCollector implements ITopologyEventCollector {
  // Note that serviceInterface may be null if there isn't an IMonitoringProducer service registered.
  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementTopologyEventCollector.class);
  private final IMonitoringProducer serviceInterface;
  private final Set<ClientID> connectedClients;
  private final Map<Long, EntityID> entities;
  private final Map<ClientID, Collection<FetchID>> incomingDisconnects;
  private final Map<ClientID, Collection<ClientInstanceID>> incomingReleases;
  private final Map<ClientID, Collection<ResolvedDescriptors>> incomingFetches;
  private boolean isActiveState;

  public ManagementTopologyEventCollector(IMonitoringProducer serviceInterface) {
    this.serviceInterface = serviceInterface;
    this.connectedClients = new HashSet<ClientID>();
    this.entities = new HashMap<Long, EntityID>();
    this.incomingReleases = new HashMap<>();
    this.incomingFetches = new HashMap<>();
    this.incomingDisconnects = new HashMap<>();
    this.isActiveState = false;
    
    // Do our initial configuration of the service.
    if (null != this.serviceInterface) {
      // Create the root of the platform tree.
      this.serviceInterface.addNode(new String[0], PlatformMonitoringConstants.PLATFORM_ROOT_NAME, null);
      // Create the root of the client subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.CLIENTS_ROOT_NAME, null);
      // Create the root of the entity subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.ENTITIES_ROOT_NAME, null);
      // Create the root of the client-entity fetch subtree.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.FETCHED_ROOT_NAME, null);
    }
  }

/**
 * it is possible for a server to update it's state multiple times without 
 *  actually changing state.  this and underlying methods must tolerate that
 */
  @Override
  public synchronized void serverDidEnterState(State state, long activateTime) {
    // We track whether or not we are in an active state to ensure that entities are created/loaded in the expected state.
    this.isActiveState = StateManager.ACTIVE_COORDINATOR.getName().equals(state.getName());
    boolean syncing = false;
    boolean standby = false;
    if (!this.isActiveState) {
      syncing = StateManager.PASSIVE_SYNCING.equals(state);
      if (!syncing) {
        standby = StateManager.PASSIVE_STANDBY.equals(state);
      }
    }

    String stateValue = this.isActiveState ? 
        PlatformMonitoringConstants.SERVER_STATE_ACTIVE : 
          (standby) ? PlatformMonitoringConstants.SERVER_STATE_PASSIVE : 
            (syncing) ? PlatformMonitoringConstants.SERVER_STATE_SYNCHRONIZING : 
                        PlatformMonitoringConstants.SERVER_STATE_UNINITIALIZED;
    
    // Set this in the monitoring interface.
    if (null != this.serviceInterface) {
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.STATE_NODE_NAME, new ServerState(stateValue, System.currentTimeMillis(), activateTime));
    }
    LOGGER.debug("server entered state " + state + " at " + activateTime);
  }

  @Override
  public synchronized void clientDidConnect(MessageChannel channel, ClientID client) {
    // Ensure that this client isn't already connected.
    Assert.assertFalse(this.connectedClients.contains(client));
    // Now, add it to the connected set.
    this.connectedClients.add(client);
    Collection<ResolvedDescriptors> earlyFetches = incomingFetches.remove(client);
    // Add it to the monitoring interface.
    if (null != this.serviceInterface) {
      // Create the structure to describe this client.
      TCSocketAddress localAddress = channel.getLocalAddress();
      TCSocketAddress remoteAddress = channel.getRemoteAddress();
      ClientHandshakeMonitoringInfo minfo = (ClientHandshakeMonitoringInfo)channel.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT);
      Assert.assertNotNull(minfo);
      PlatformConnectedClient clientDescription = new PlatformConnectedClient(
          minfo.getUuid(),
          minfo.getName(),
          localAddress.getAddress(), localAddress.getPort(), remoteAddress.getAddress(), remoteAddress.getPort(),
              minfo.getPid() & 0xffffffffL);
      // We will use the ClientID long value as the node name.
      String nodeName = clientIdentifierForService(client);
      this.serviceInterface.addNode(PlatformMonitoringConstants.CLIENTS_PATH, nodeName, clientDescription);
      if (earlyFetches != null && !earlyFetches.isEmpty()) {
        for (ResolvedDescriptors ed : earlyFetches) {
          String fetchIdentifier = fetchIdentifierForService(client, ed.id, ed.consumerID, ed.instance);
          boolean didAdd = this.serviceInterface.addNode(PlatformMonitoringConstants.FETCHED_PATH, 
                  fetchIdentifier, 
                  new PlatformClientFetchedEntity(clientIdentifierForService(client), 
                          entityIdentifierForService(ed.id, ed.consumerID), 
                          new ClientDescriptorImpl(client, ed.getClientInstanceID())));
          // This MUST have been added (otherwise, it implies that there is a serious bug somewhere).
          if (!didAdd) {
            LOGGER.warn("unbalanced client connect " + fetchIdentifier);
          }        
        }
        
      }
    }
    LOGGER.debug("client did connect " + channel);
  }

  @Override
  public synchronized void clientDidDisconnect(ClientID client) {
    // Ensure that this client was already connected.
    Assert.assertTrue(this.connectedClients.contains(client));
    // Now, remove it from the connected set.
    this.connectedClients.remove(client);
    
    // Remove it from the monitoring interface.
    removeClientIfPossible(client);
    LOGGER.debug("client did disconnect " + client);
  }

  @Override
  public synchronized void entityWasCreated(EntityID id, long consumerID, boolean isActive) {
    // Ensure that this is the expected state.
    Assert.assertTrue(isActive == this.isActiveState);
    // Ensure that this entity didn't already exist.
    Assert.assertFalse(this.entities.containsKey(consumerID));
    addEntityToTracking(id, consumerID, isActive);
    LOGGER.debug("entity created " + id);
  }

  @Override
  public synchronized void entityWasDestroyed(EntityID id, long consumerID) {
    // Ensure that this entity already exists.
    Assert.assertTrue(this.entities.containsKey(consumerID));
    // Now, remove it from the set.
    removeEntityFromTracking(id, consumerID);
    LOGGER.debug("entity destroyed " + id);
  }

  @Override
  public synchronized void entityWasReloaded(EntityID id, long consumerID, boolean isActive) {
    // terracotta-core issue-461:  reconfigured entities should be re-added, not remove-then-add.
    addEntityToTracking(id, consumerID, isActive);
    LOGGER.debug("entity reloaded " + id);
  }

  @Override
  public synchronized void clientDidFetchEntity(ClientID client, EntityID entity, long consumerID, ClientInstanceID instance) {
    // Add it to the monitoring interface.
    if (null != this.serviceInterface) {
      String clientIdentifier = clientIdentifierForService(client);
      String entityIdentifier = entityIdentifierForService(entity, consumerID);
      PlatformClientFetchedEntity record = new PlatformClientFetchedEntity(clientIdentifier, entityIdentifier, new ClientDescriptorImpl(client, instance));
      if (connectedClients.contains(client)) {
        String fetchIdentifier = fetchIdentifierForService(client, entity, consumerID, instance);
        boolean didAdd = this.serviceInterface.addNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier, record);
        // This MUST have been added (otherwise, it implies that there is a serious bug somewhere).
        if (!didAdd) {
          LOGGER.warn("unbalanced client fetch " + fetchIdentifier);
        }
      } else {
        Collection<ResolvedDescriptors> set = incomingFetches.computeIfAbsent(client, (c)->new HashSet<>());
        set.add(new ResolvedDescriptors(entity, consumerID, instance));
      }
    }
    LOGGER.debug("client " + client + " fetched " + instance);
  }

  @Override
  public synchronized void clientDidReleaseEntity(ClientID client, EntityID entity, long consumerID, ClientInstanceID instance) {
    // Remove it from the monitoring interface.
    if (null != this.serviceInterface) {
      String fetchIdentifier = fetchIdentifierForService(client, entity, consumerID, instance);
      boolean didRemove = this.serviceInterface.removeNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier);
      // This CANNOT be unbalanced (it implies that there is a serious bug somewhere).
      if (!didRemove) {
        LOGGER.warn("unbalanced client release " + fetchIdentifier);
      }
    }
    
    if (incomingReleases.containsKey(client)) {
      Collection<ClientInstanceID> expected = incomingReleases.get(client);
      Assert.assertTrue(expected.removeIf(des->des.equals(instance)));
      if (expected.isEmpty()) {
        incomingReleases.remove(client);
        removeClientIfPossible(client);
      }
    }
    LOGGER.debug("client " + client + " released " + entity);
  }
  
  public synchronized void expectedDisconnects(ClientID cid, Collection<FetchID> releases) {
    if (null != serviceInterface) {
      if (!releases.isEmpty()) {
        incomingDisconnects.put(cid, new ArrayList<>(releases));
      }
      removeClientIfPossible(cid);
    }
  }  
  
  public synchronized void clientDisconnectedFromEntity(ClientID cid, FetchID fetch, Collection<EntityDescriptor> fids) {
    if (null != serviceInterface) {
      Collection<FetchID> fetches = incomingDisconnects.get(cid);
      expectedReleases(cid, fids);
      Assert.assertTrue(fetches.remove(fetch));
      if (fetches.isEmpty()) {
        incomingDisconnects.remove(cid);
        removeClientIfPossible(cid);
      }
    }
  }
  
  private void removeClientIfPossible(ClientID client) {
    if (!incomingReleases.containsKey(client) && !incomingDisconnects.containsKey(client)) {
      // Remove it from the monitoring interface.
      if (null != this.serviceInterface) {
        String nodeName = clientIdentifierForService(client);
        this.serviceInterface.removeNode(PlatformMonitoringConstants.CLIENTS_PATH, nodeName);
      }
    }
  }
  
  private void expectedReleases(ClientID cid, Collection<EntityDescriptor> releases) {
    if (!releases.isEmpty()) {
      incomingReleases.compute(cid, (ignore,ex)->{
        if (ex == null) {
          ex = new HashSet<>();
        }
        ex.addAll(releases.stream().map(EntityDescriptor::getClientInstanceID).collect(Collectors.toList()));
        return ex;
      });
    }
  }


  private void addEntityToTracking(EntityID id, long consumerID, boolean isActive) {
    this.entities.put(consumerID, id);
    
    // Add it to the monitoring interface.
    if (null != this.serviceInterface) {
      String entityClassName = id.getClassName();
      String entityName = id.getEntityName();
      PlatformEntity record = new PlatformEntity(entityClassName, entityName, consumerID, isActive);
      String entityIdentifier = entityIdentifierForService(id, consumerID);
      this.serviceInterface.addNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier, record);
    }
  }

  private void removeEntityFromTracking(EntityID id, long consumerID) {
    this.entities.remove(consumerID);
    
    // Remove it to the monitoring interface.
    if (null != this.serviceInterface) {
      String entityIdentifier = entityIdentifierForService(id, consumerID);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier);
    }
  }

  private String clientIdentifierForService(ClientID id) {
    return "" + id.toLong();
  }

  private String entityIdentifierForService(EntityID id, long consumerID) {
    return id.getClassName() + id.getEntityName() + consumerID;
  }
  
  private String fetchIdentifierForService(ClientID client, EntityID entity, long consumerID, ClientInstanceID cid) {
    return clientIdentifierForService(client) + "_" + entityIdentifierForService(entity, consumerID) + "_" + cid.getID(); 
  }
  
  private static class ResolvedDescriptors {
    private final EntityID   id;
    private final long consumerID;
    private final ClientInstanceID instance;

    public ResolvedDescriptors(EntityID entityID, long consumerID, ClientInstanceID instance) {
      this.id = entityID;
      this.consumerID = consumerID;
      this.instance = instance;
    }

    public long getConsumerID() {
      return consumerID;
    }

    public ClientInstanceID getClientInstanceID() {
      return instance;
    }

    public EntityID getEntityID() {
      return id;
    }
  }
}
