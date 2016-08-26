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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformMonitoringConstants;

import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityID;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.server.TCServerMain;
import com.tc.util.Assert;
import com.tc.util.State;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;


/**
 * The basic implementation of ITopologyEventCollector which ensures that events aren't obviously incorrect by ensuring
 * consistent state and symmetry of events.
 */
public class ManagementTopologyEventCollector implements ITopologyEventCollector {
  // Note that serviceInterface may be null if there isn't an IMonitoringProducer service registered.
  private final IMonitoringProducer serviceInterface;
  private final Set<ClientID> connectedClients;
  private final Set<EntityID> entities;
  private final Map<NodeID, PlatformServer> servers;
  private final Map<FetchTuple, Integer> fetchPairCounts;
  private boolean isActiveState;
    
  private final ServerID thisNode;

  private static final TCLogger LOGGER = TCLogging.getLogger(ManagementTopologyEventCollector.class);

  public ManagementTopologyEventCollector(ServerID self, IMonitoringProducer serviceInterface) {
    this.thisNode = self;
    this.serviceInterface = serviceInterface;
    this.connectedClients = new HashSet<ClientID>();
    this.entities = new HashSet<EntityID>();
    this.fetchPairCounts = new HashMap<FetchTuple, Integer>();
    this.servers = new HashMap<NodeID, PlatformServer>();
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
      // Create the initial server state.
      this.serviceInterface.addNode(PlatformMonitoringConstants.PLATFORM_PATH, PlatformMonitoringConstants.SERVERS_ROOT_NAME, null);
    }
  }
//  server information is communicated in a broadcast fashion
//  from multiple pathways.  It is possible to get duplicate information and 
//  this method must tolerate that
  @Override
  public synchronized void serverDidJoinGroup(ServerID node, String serverName, String hostname, 
      String bindAddress, int bindPort, int groupPort, String version, String build) {
    String hostAddress = "";
    try {
      hostAddress = java.net.InetAddress.getByName(hostname).getHostAddress();
    } catch (UnknownHostException unknown) {
      // ignore
    }
    PlatformServer server = new PlatformServer(serverName, hostname, hostAddress, bindAddress, bindPort, groupPort, version, build, TCServerMain.getServer().getStartTime()); 
    if (this.servers.put(node, server) == null) {
      LOGGER.debug("adding NODE:" + Arrays.toString(PlatformMonitoringConstants.SERVERS_PATH) + " NAME:" + serverIdentifierForService(node) + " VALUE:" + server);
      if (null != this.serviceInterface) {
        this.serviceInterface.addNode(PlatformMonitoringConstants.SERVERS_PATH, serverIdentifierForService(node), server);
      }
    } else {
      LOGGER.debug("adding an already existing server node:" + node);
    }
  }
//  server information is communicated in a broadcast fashion
//  from multiple pathways.  It is possible to get duplicate information and 
//  this method must tolerate that
  @Override
  public synchronized void serverDidLeaveGroup(ServerID node) {
    if (this.servers.remove(node) != null) {
      LOGGER.debug("removing NODE:" + Arrays.toString(PlatformMonitoringConstants.SERVERS_PATH) + " NAME:" + serverIdentifierForService(node));
      if (this.serviceInterface != null) {
        this.serviceInterface.removeNode(PlatformMonitoringConstants.SERVERS_PATH, serverIdentifierForService(node));
      }
    } else {
      LOGGER.info("removing non-existent server node " + node);
    }
  }
/**
 * it is possible for a server to update it's state multiple times without 
 *  actually changing state.  this and underlying methods must tolerate that
 */
  @Override
  public synchronized void serverDidEnterState(ServerID node, State state, long activateTime) {
    // We track whether or not we are in an active state to ensure that entities are created/loaded in the expected state.
    if (node.equals(thisNode)) {
      this.isActiveState = StateManager.ACTIVE_COORDINATOR.getName().equals(state.getName());
    }
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
    
    LOGGER.debug("state NODE:" + serverIdentifierForService(node) + " announcing state " + stateValue);
    // Set this in the monitoring interface.
    if (null != this.serviceInterface) {
      this.serviceInterface.removeNode(makeServerPath(node), PlatformMonitoringConstants.STATE_NODE_NAME);
      this.serviceInterface.addNode(makeServerPath(node), PlatformMonitoringConstants.STATE_NODE_NAME, new ServerState(stateValue, System.currentTimeMillis(), activateTime));
    }
  }

  private String[] makeServerPath(ServerID node, String...slot) {
    String[] path = Arrays.copyOf(PlatformMonitoringConstants.SERVERS_PATH, PlatformMonitoringConstants.SERVERS_PATH.length + 1 + slot.length);
    path[PlatformMonitoringConstants.SERVERS_PATH.length] = serverIdentifierForService(node);
    System.arraycopy(slot, 0, path, PlatformMonitoringConstants.SERVERS_PATH.length + 1, slot.length);
    return path;
  }
  
  @Override
  public synchronized void clientDidConnect(MessageChannel channel, ClientID client) {
    // Ensure that this client isn't already connected.
    Assert.assertFalse(this.connectedClients.contains(client));
    // Now, add it to the connected set.
    this.connectedClients.add(client);
    
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
    }
  }

  @Override
  public synchronized void clientDidDisconnect(MessageChannel channel, ClientID client) {
    // Ensure that this client was already connected.
    Assert.assertTrue(this.connectedClients.contains(client));
    // Now, remove it from the connected set.
    this.connectedClients.remove(client);
    
    // Remove it from the monitoring interface.
    if (null != this.serviceInterface) {
      String nodeName = clientIdentifierForService(client);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.CLIENTS_PATH, nodeName);
    }
  }

  @Override
  public synchronized void entityWasCreated(EntityID id, long consumerID, boolean isActive) {
    // Ensure that this is the expected state.
    Assert.assertTrue(isActive == this.isActiveState);
    // Ensure that this entity didn't already exist.
    Assert.assertFalse(this.entities.contains(id));
    addEntityToTracking(id, consumerID, isActive);
  }

  @Override
  public synchronized void entityWasDestroyed(EntityID id) {
    // Ensure that this entity already exists.
    Assert.assertTrue(this.entities.contains(id));
    // Now, remove it from the set.
    removeEntityFromTracking(id);
  }

  @Override
  public synchronized void entityWasReloaded(EntityID id, long consumerID, boolean isActive) {
    // Ensure that this is the expected state.
    // NOTE:  We currently can't verify the isActiveState since the reload path sets it _after_ the entities are reloaded.
    // Note that this could happen due to promotion or reloading from restart so we can't know if it already is in our set.
    if (!this.entities.contains(id)) {
      // Seems to be new so add it to the set.
      addEntityToTracking(id, consumerID, isActive);
    }
  }

  @Override
  public synchronized void clientDidFetchEntity(ClientID client, EntityID id, ClientDescriptor clientDescriptor) {
    // XXX: Note that there is currently no handling of reconnect on entity promotion from passive to active so there may
    // be double-counting, here.
    FetchTuple tuple = new FetchTuple(client, id);
    int count = 0;
    if (this.fetchPairCounts.containsKey(tuple)) {
      count = this.fetchPairCounts.get(tuple);
    }
    count += 1;
    this.fetchPairCounts.put(tuple, count);
    
    // Add it to the monitoring interface.
    if (null != this.serviceInterface) {
      String clientIdentifier = clientIdentifierForService(client);
      String entityIdentifier = entityIdentifierForService(id);
      PlatformClientFetchedEntity record = new PlatformClientFetchedEntity(clientIdentifier, entityIdentifier, clientDescriptor);
      String fetchIdentifier = fetchIdentifierForService(clientIdentifier, entityIdentifier);
      this.serviceInterface.addNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier, record);
    }
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
    
    // Remove it from the monitoring interface.
    if (null != this.serviceInterface) {
      String clientIdentifier = clientIdentifierForService(client);
      String entityIdentifier = entityIdentifierForService(id);
      String fetchIdentifier = fetchIdentifierForService(clientIdentifier, entityIdentifier);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.FETCHED_PATH, fetchIdentifier);
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


  private void addEntityToTracking(EntityID id, long consumerID, boolean isActive) {
    this.entities.add(id);
    
    // Add it to the monitoring interface.
    if (null != this.serviceInterface) {
      String entityClassName = id.getClassName();
      String entityName = id.getEntityName();
      PlatformEntity record = new PlatformEntity(entityClassName, entityName, consumerID, isActive);
      String entityIdentifier = entityIdentifierForService(id);
      this.serviceInterface.addNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier, record);
    }
  }

  private void removeEntityFromTracking(EntityID id) {
    this.entities.remove(id);
    
    // Remove it to the monitoring interface.
    if (null != this.serviceInterface) {
      String entityIdentifier = entityIdentifierForService(id);
      this.serviceInterface.removeNode(PlatformMonitoringConstants.ENTITIES_PATH, entityIdentifier);
    }
  }

  private String clientIdentifierForService(ClientID id) {
    return "" + id.toLong();
  }

  private String serverIdentifierForService(ServerID id) {
    return uidToString(id.getUID()) + "(" + id.getName() + ")";
  }  
  
  private String uidToString(byte[] id) {
    StringBuilder convert = new StringBuilder();
    for (byte b : id) {
      convert.append(Integer.toHexString(0xff & b));
    }
    return convert.toString();
  }

  private String entityIdentifierForService(EntityID id) {
    return id.getClassName() + id.getEntityName();
  }

  private String fetchIdentifierForService(String clientIdentifier, String entityIdentifier) {
    return clientIdentifier + entityIdentifier;
  }
}
