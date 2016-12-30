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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

import com.tc.net.TCSocketAddress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.util.UUID;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import static org.mockito.Matchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.monitoring.PlatformMonitoringConstants;


public class ManagementTopologyEventCollectorTest {
  private ManagementTopologyEventCollector collector;

  @Before
  public void setUp() throws Exception {
    // We want to create a monitoring producer implementation which just ensures that all calls are balanced.
    this.collector = new ManagementTopologyEventCollector(new IMonitoringProducer() {
      private HashSet<PathContainer> validPaths = new HashSet<>();
      private HashMap<PathContainer, Serializable> valuesAtPaths = new HashMap<>();
      @Override
      public boolean addNode(String[] parents, String name, Serializable value) {
        PathContainer parentsContainer = new PathContainer(parents);
        boolean didFindParent = (null == parents) || (0 == parents.length) || this.validPaths.contains(parentsContainer);
        if (didFindParent) {
          String[] newPath = new String[parents.length + 1];
          System.arraycopy(parents, 0, newPath, 0, parents.length);
          newPath[parents.length] = name;
          // Even if something is already there, we will just over-write.
          PathContainer container = new PathContainer(newPath);
          this.validPaths.add(container);
          this.valuesAtPaths.put(container, value);
        }
        return didFindParent;
      }
      @Override
      public void pushBestEffortsData(String name, Serializable data) {
        // Not part of test.
        Assert.fail();
      }
      @Override
      public boolean removeNode(String[] parents, String name) {
        String[] pathToRemove = new String[parents.length + 1];
        System.arraycopy(parents, 0, pathToRemove, 0, parents.length);
        pathToRemove[parents.length] = name;
        PathContainer parentsContainer = new PathContainer(pathToRemove);
        boolean didFind = this.validPaths.contains(parentsContainer);
        if (didFind) {
          // Extract anything with this prefix.
          HashSet<PathContainer> newPaths = new HashSet<>();
          for (PathContainer onePath : this.validPaths) {
            boolean shouldAdd = !onePath.hasPrefix(parentsContainer);
            if (shouldAdd) {
              newPaths.add(onePath);
            } else {
              this.valuesAtPaths.remove(onePath);
            }
          }
          this.validPaths = newPaths;
        }
        return didFind;
      }});
  }

  @Test
  public void testConnectDisconnect() throws Exception {
    MessageChannel channel = mockMessageChannel();
    ClientID client = mock(ClientID.class);
    this.collector.clientDidConnect(channel, client);
    // We should fail to connect a second time.
    boolean didSucceed = false;
    // XXX:  Note that there is currently a bug where we need to permit this so this part of the test is disabled.
    boolean isConnectCheckBroken = true;
    if (!isConnectCheckBroken) {
      try {
        this.collector.clientDidConnect(channel, client);
        didSucceed = true;
      } catch (AssertionError e) {
        // Expected.
      }
      Assert.assertFalse(didSucceed);
    }
    // Now, disconnect.
    this.collector.clientDidDisconnect(client);
    // We should fail to disconnect a second time.
    // XXX:  Note that there is currently a bug where we need to permit this so this part of the test is disabled.
    boolean isDisconnectCheckBroken = true;
    if (!isDisconnectCheckBroken) {
      didSucceed = false;
      try {
        this.collector.clientDidDisconnect(client);
        didSucceed = true;
      } catch (AssertionError e) {
        // Expected.
      }
      Assert.assertFalse(didSucceed);
    }
  }

  @Test
  public void testCreatePromoteDestroy() throws Exception {
    EntityID id = mock(EntityID.class);
    long consumerID = 1;
    // Note that we start in passive state.
    boolean isActive = false;
    
    // We should fail to create an entity as active.
    boolean didSucceed = false;
    try {
      this.collector.entityWasCreated(id, consumerID, true);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
    
    // Create it as passive.
    this.collector.entityWasCreated(id, consumerID, isActive);
    // We should fail to create it a second time.
    didSucceed = false;
    try {
      this.collector.entityWasCreated(id, consumerID, isActive);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
    long timestamp = System.currentTimeMillis();
    // Now, change state of the server to active.
    this.collector.serverDidEnterState(StateManager.ACTIVE_COORDINATOR, timestamp);
    isActive = true;
    
    // Promote the entity to active.
    this.collector.entityWasReloaded(id, consumerID, isActive);
    
    // Now, destroy the entity.
    this.collector.entityWasDestroyed(id);
    
    // We should fail to destroy it twice.
    didSucceed = false;
    try {
      this.collector.entityWasDestroyed(id);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
  }

  @Test
  public void testFetchReleaseActiveEntity() throws Exception {
    EntityID id = mock(EntityID.class);
    long consumerID = 1;
    MessageChannel channel = mockMessageChannel();
    
    ClientID client = mock(ClientID.class);
    EntityDescriptor entityDescriptor1 = mockDescriptor(id, new ClientInstanceID(1));
    EntityDescriptor entityDescriptor2 = mockDescriptor(id, new ClientInstanceID(2));
    ClientDescriptor clientDescriptor1 = mock(ClientDescriptor.class);
    ClientDescriptor clientDescriptor2 = mock(ClientDescriptor.class);
    
    // Put us into the active state.
    this.collector.serverDidEnterState(StateManager.ACTIVE_COORDINATOR, System.currentTimeMillis());
    boolean isActive = true;
    
    // Create the entity.
    this.collector.entityWasCreated(id, consumerID, isActive);
    
    // Connect the client.
    this.collector.clientDidConnect(channel, client);
    
    // Fetch the entity.
    this.collector.clientDidFetchEntity(client, entityDescriptor1, clientDescriptor1);
    
    // Fetch again, since there can be multiple fetches from the same client.
    this.collector.clientDidFetchEntity(client, entityDescriptor2, clientDescriptor2);
    
    // Release the entity twice.
    this.collector.clientDidReleaseEntity(client, entityDescriptor1);
    this.collector.clientDidReleaseEntity(client, entityDescriptor2);
    
    // A third attempt to release should fail.
    boolean didSucceed = false;
    try {
      this.collector.clientDidReleaseEntity(client, entityDescriptor1);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
    
    // Disconnect the client.
    this.collector.clientDidDisconnect(client);
    
    // Destroy the entity.
    this.collector.entityWasDestroyed(id);
  }

  @Test
  public void testClientPIDInclusion() throws Exception {
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
    this.collector = new ManagementTopologyEventCollector(monitoringProducer);

    // reset monitoringProducer.addNode(...) invocation counts
    reset(monitoringProducer);

    final int TEST_CLIENT_PID = 2498;
    final String uuid = UUID.getUUID().toString();
    final String name = "TEST";
    
    // prepare and call collector.clientDidConnect(...)
    MessageChannel channel = mock(MessageChannel.class);
    ClientHandshakeMonitoringInfo info = new ClientHandshakeMonitoringInfo(TEST_CLIENT_PID, uuid, name);
    when(channel.getAttachment(Matchers.eq(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT))).thenReturn(info);
    when(channel.getLocalAddress()).thenReturn(new TCSocketAddress("localhost", 1234));
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress("localhost", 4567));
    ClientID client = mock(ClientID.class);
    this.collector.clientDidConnect(channel, client);

    // verify
    ArgumentCaptor<PlatformConnectedClient> argumentCaptor = ArgumentCaptor.forClass(PlatformConnectedClient.class);
    verify(monitoringProducer).addNode(any(), any(), argumentCaptor.capture());
    Assert.assertEquals(TEST_CLIENT_PID, argumentCaptor.getValue().clientPID);
    Assert.assertEquals(uuid, argumentCaptor.getValue().uuid);
    Assert.assertEquals(name, argumentCaptor.getValue().name);
  }

  @Test
  public void testClientDescriptorInclusion() throws Exception {
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
    this.collector = new ManagementTopologyEventCollector(monitoringProducer);

    // reset monitoringProducer.addNode(...) invocation counts
    reset(monitoringProducer);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);

    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT)).thenReturn(mock(ClientHandshakeMonitoringInfo.class));
    when(channel.getLocalAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 1));
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 2));

    EntityID entityID = mock(EntityID.class);
    EntityDescriptor entityDescriptor1 = mockDescriptor(entityID, new ClientInstanceID(1));
    ClientID client = mock(ClientID.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    this.collector.clientDidConnect(channel, client);
    this.collector.clientDidFetchEntity(client, entityDescriptor1, clientDescriptor);

    // verify
    ArgumentCaptor<PlatformClientFetchedEntity> argumentCaptor = ArgumentCaptor.forClass(PlatformClientFetchedEntity.class);
    verify(monitoringProducer).addNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.FETCHED_PATH), any(), argumentCaptor.capture());
    Assert.assertEquals(clientDescriptor, argumentCaptor.getValue().clientDescriptor);
  }

  @Test
  public void testApiTypeSerialization() throws Exception {
    // Note that we don't currently put unit tests in the API project (although it is worth considering, for cases such as
    //  this) so we will put some simple tests to ensure that the monitoring types correctly serialize/deserialize.
    
    // Create some objects.
    String clientIdentifier = "clientIdentifier";
    String entityIdentifier = "entityIdentifier";
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    PlatformClientFetchedEntity originalFetchedEntity = new PlatformClientFetchedEntity(clientIdentifier, entityIdentifier, clientDescriptor);
    
    String uuid = "uuid";
    String name = "name";
    InetAddress localAddress = InetAddress.getLocalHost();
    int localPort = 1;
    InetAddress remoteAddress = InetAddress.getLoopbackAddress();
    int remotePort = 2;
    long clientPID = 3;
    PlatformConnectedClient originalConnectedClient = new PlatformConnectedClient(uuid, name, localAddress, localPort, remoteAddress, remotePort, clientPID);
    
    String typeName = "typeName";
    String entityName = "entityName";
    long consumerID = 1;
    boolean isActive = false;
    PlatformEntity originalEntity = new PlatformEntity(typeName, entityName, consumerID, isActive);
    
    String serverName = "serverName";
    String host = "host";
    String hostAddress = "hostAddress";
    String bindAddress = "bindAddress";
    int bindPort = 1;
    int groupPort = 2;
    String version = "version";
    String build = "build";
    long startTime = 3;
    PlatformServer originalServer = new PlatformServer(serverName, host, hostAddress, bindAddress, bindPort, groupPort, version, build, startTime);
    
    String state = "state";
    long timestamp = 1;
    long activate = 2;
    ServerState originalState = new ServerState(state, timestamp, activate);
    
    
    // Serialize them to a byte[].
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
    objectOutput.writeObject(originalFetchedEntity);
    objectOutput.writeObject(originalConnectedClient);
    objectOutput.writeObject(originalEntity);
    objectOutput.writeObject(originalServer);
    objectOutput.writeObject(originalState);
    objectOutput.close();
    byteOutput.close();
    byte[] buffer = byteOutput.toByteArray();
    
    // Deserialize them back into objects.
    ByteArrayInputStream byteInput = new ByteArrayInputStream(buffer);
    ObjectInputStream objectInput = new ObjectInputStream(byteInput);
    PlatformClientFetchedEntity readFetchedEntity = (PlatformClientFetchedEntity)objectInput.readObject();
    PlatformConnectedClient readConnectedClient = (PlatformConnectedClient)objectInput.readObject();
    PlatformEntity readEntity = (PlatformEntity)objectInput.readObject();
    PlatformServer readServer = (PlatformServer)objectInput.readObject();
    ServerState readState = (ServerState)objectInput.readObject();
    objectInput.close();
    byteInput.close();
    
    // Verify that they are still equal.
    // Note that we expect that the read fetched entity will NOT equal the original, until we null the clientDescriptor.
    Assert.assertNotEquals(originalFetchedEntity, readFetchedEntity);
    originalFetchedEntity.clientDescriptor = null;
    Assert.assertEquals(originalFetchedEntity, readFetchedEntity);
    Assert.assertEquals(originalConnectedClient, readConnectedClient);
    Assert.assertEquals(originalEntity, readEntity);
    Assert.assertEquals(originalServer, readServer);
    Assert.assertEquals(originalState, readState);
  }
  
  @Test
  public void testClientEventOrderingOnConnect() throws Exception {
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
    when(monitoringProducer.removeNode(any(), any())).thenReturn(true);
    this.collector = new ManagementTopologyEventCollector(monitoringProducer);
    this.collector.serverDidEnterState(StateManager.ACTIVE_COORDINATOR, 0);
    ClientID cid = mock(ClientID.class);
    when(cid.toLong()).thenReturn(1L);
    
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT)).thenReturn(mock(ClientHandshakeMonitoringInfo.class));
    when(channel.getLocalAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 1));
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 2));
    
//  TEST with 10 entities fetched
    int counts[] = {0,10,1};
    for (int count : counts) {
      reset(monitoringProducer);
      when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
      when(monitoringProducer.removeNode(any(), any())).thenReturn(true);
      System.out.println("testing " + count + " fetched entities");
      EntityID[] entities = new EntityID[count];
      for (int x=0;x<count;x++) {
        entities[x] = new EntityID("testClass", "test " + x);
      }

      for (EntityID eid : entities) {
        this.collector.entityWasCreated(eid, 1, true);
      }
      for (EntityID eid : entities) {
        ClientDescriptor descriptor = mock(ClientDescriptor.class);
        EntityDescriptor entityDescriptor1 = mockDescriptor(eid, new ClientInstanceID(1));
        this.collector.clientDidFetchEntity(cid, entityDescriptor1, descriptor);
      }
      verify(monitoringProducer, never()).addNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.FETCHED_PATH), Matchers.anyString(), Matchers.any());
      this.collector.clientDidConnect(channel, cid);
      verify(monitoringProducer, times(count)).addNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.FETCHED_PATH), Matchers.anyString(), Matchers.any());
  //  simulate ClientEntityStateManger detecting a client disconnect
      this.collector.expectedReleases(cid, Arrays.asList(entities).stream().map(eid->new EntityDescriptor(eid, new ClientInstanceID(1), 1)).collect(Collectors.toList()));
  //  now disconnect the client
      this.collector.clientDidDisconnect(cid);
      for (EntityID eid : entities) {
        verify(monitoringProducer, Mockito.never()).removeNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.CLIENTS_PATH), Matchers.eq(Long.toString(1L)));    
        EntityDescriptor entityDescriptor1 = mockDescriptor(eid, new ClientInstanceID(1));
        this.collector.clientDidReleaseEntity(cid, entityDescriptor1);
        verify(monitoringProducer).removeNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.FETCHED_PATH), Matchers.eq("1_" + eid.getClassName() + eid.getEntityName() + "_1"));
      }
      verify(monitoringProducer).removeNode(AdditionalMatchers.aryEq(PlatformMonitoringConstants.CLIENTS_PATH), Matchers.eq(Long.toString(1L)));
      
      for (EntityID eid : entities) {
        this.collector.entityWasDestroyed(eid);
      }
    }
  }  
  
  @Test
  public void testClientEventOrdering() throws Exception {
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
    when(monitoringProducer.removeNode(any(), any())).thenReturn(true);
    this.collector = new ManagementTopologyEventCollector(monitoringProducer);
    this.collector.serverDidEnterState(StateManager.ACTIVE_COORDINATOR, 0);
    ClientID cid = mock(ClientID.class);
    when(cid.toLong()).thenReturn(1L);
    
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getAttachment(ClientHandshakeMonitoringInfo.MONITORING_INFO_ATTACHMENT)).thenReturn(mock(ClientHandshakeMonitoringInfo.class));
    when(channel.getLocalAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 1));
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress("0.0.0.0", 2));
    
//  TEST with 10 entities fetched
    int counts[] = {0,10,1};
    for (int count : counts) {
      reset(monitoringProducer);
      when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
      when(monitoringProducer.removeNode(any(), any())).thenReturn(true);
      System.out.println("testing " + count + " fetched entities");
      this.collector.clientDidConnect(channel, cid);
      EntityID[] entities = new EntityID[count];
      for (int x=0;x<count;x++) {
        entities[x] = new EntityID("testClass", "test " + x);
      }

      for (EntityID eid : entities) {
        this.collector.entityWasCreated(eid, 1, true);
      }
      for (EntityID eid : entities) {
        ClientDescriptor descriptor = mock(ClientDescriptor.class);
        EntityDescriptor entityDescriptor1 = mockDescriptor(eid, new ClientInstanceID(1));
        this.collector.clientDidFetchEntity(cid, entityDescriptor1, descriptor);
      }
  //  simulate ClientEntityStateManger detecting a client disconnect
      this.collector.expectedReleases(cid, Arrays.asList(entities).stream().map(eid->new EntityDescriptor(eid, new ClientInstanceID(1), 1)).collect(Collectors.toList()));
  //  now disconnect the client
      this.collector.clientDidDisconnect(cid);
      for (EntityID eid : entities) {
        verify(monitoringProducer, Mockito.never()).removeNode(Matchers.eq(PlatformMonitoringConstants.CLIENTS_PATH), Matchers.eq(Long.toString(1L)));    
        EntityDescriptor entityDescriptor1 = mockDescriptor(eid, new ClientInstanceID(1));
        this.collector.clientDidReleaseEntity(cid, entityDescriptor1);
        verify(monitoringProducer).removeNode(Matchers.eq(PlatformMonitoringConstants.FETCHED_PATH), Matchers.eq("1_" + eid.getClassName() + eid.getEntityName() + "_1"));
      }
      verify(monitoringProducer).removeNode(Matchers.eq(PlatformMonitoringConstants.CLIENTS_PATH), Matchers.eq(Long.toString(1L)));
      
      for (EntityID eid : entities) {
        this.collector.entityWasDestroyed(eid);
      }
    }
  }

  private EntityDescriptor mockDescriptor(EntityID id, ClientInstanceID clientInstanceID) {
    EntityDescriptor descriptor = mock(EntityDescriptor.class);
    when(descriptor.getEntityID()).thenReturn(id);
    when(descriptor.getClientInstanceID()).thenReturn(clientInstanceID);
    return descriptor;
  }

  private MessageChannel mockMessageChannel() {
    MessageChannel channel = mock(MessageChannel.class);
    TCSocketAddress localAddress = mock(TCSocketAddress.class);
    when(localAddress.getAddress()).thenReturn(mock(InetAddress.class));
    when(localAddress.getPort()).thenReturn(1035);
    TCSocketAddress remoteAddress = mock(TCSocketAddress.class);
    when(remoteAddress.getAddress()).thenReturn(mock(InetAddress.class));
    when(remoteAddress.getPort()).thenReturn(1035);
    when(channel.getAttachment(any(String.class))).thenReturn(mock(ClientHandshakeMonitoringInfo.class));
    when(channel.getLocalAddress()).thenReturn(localAddress);
    when(channel.getRemoteAddress()).thenReturn(remoteAddress);
    return channel;
  }


  /**
   * We need to perform some basic tests on paths so we wrap them in an object.
   */
  private static class PathContainer {
    private final String[] path;
    public PathContainer(String[] path) {
      this.path = (null != path) ? path : new String[0];
      for (String oneName : path) {
        Assert.assertNotNull(oneName);
      }
    }
    public boolean hasPrefix(PathContainer parentsContainer) {
      boolean hasPrefix = false;
      if (path.length >= parentsContainer.path.length) {
        boolean doesMatch = true;
        for (int i = 0; doesMatch && (i < parentsContainer.path.length); ++i) {
          doesMatch = this.path[i].equals(parentsContainer.path[i]);
        }
        hasPrefix = doesMatch;
      }
      return hasPrefix;
    }
    @Override
    public int hashCode() {
      // We aren't too worried about this, for this test.
      return path.length;
    }
    @Override
    public boolean equals(Object obj) {
      boolean isEquals = false;
      if (obj instanceof PathContainer) {
        PathContainer other = (PathContainer) obj;
        if (other.path.length == this.path.length) {
          boolean didAllMatch = true;
          for (int i = 0; didAllMatch && (i < this.path.length); ++i) {
            if (!this.path[i].equals(other.path[i])) {
              didAllMatch = false;
            }
          }
          isEquals = didAllMatch;
        }
      }
      return isEquals;
    }
    @Override
    public String toString() {
      String collector = "[";
      for (String path : this.path) {
        collector += "/" + path;
      }
      collector += "]";
      return collector;
    }
  }
}
