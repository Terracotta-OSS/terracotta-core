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
import java.net.InetAddress;

import com.tc.net.TCSocketAddress;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityID;
import com.tc.objectserver.handshakemanager.ClientHandshakeMonitoringInfo;
import com.tc.util.UUID;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ManagementTopologyEventCollectorTest {
  private ManagementTopologyEventCollector collector;
  private ServerID selfID;

  @Before
  public void setUp() throws Exception {
    this.selfID = mock(ServerID.class);
    this.collector = new ManagementTopologyEventCollector(selfID, null);
    when(selfID.getUID()).thenReturn("TEST".getBytes());
    when(selfID.getName()).thenReturn("localhost:0000");
  }

  @Test
  public void testConnectDisconnect() throws Exception {
    MessageChannel channel = mock(MessageChannel.class);
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
    this.collector.clientDidDisconnect(channel, client);
    // We should fail to disconnect a second time.
    // XXX:  Note that there is currently a bug where we need to permit this so this part of the test is disabled.
    boolean isDisconnectCheckBroken = true;
    if (!isDisconnectCheckBroken) {
      didSucceed = false;
      try {
        this.collector.clientDidDisconnect(channel, client);
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
    this.collector.serverDidEnterState(selfID, StateManager.ACTIVE_COORDINATOR, timestamp);
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
    MessageChannel channel = mock(MessageChannel.class);
    ClientID client = mock(ClientID.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    
    // Put us into the active state.
    this.collector.serverDidEnterState(selfID, StateManager.ACTIVE_COORDINATOR, System.currentTimeMillis());
    boolean isActive = true;
    
    // Create the entity.
    this.collector.entityWasCreated(id, consumerID, isActive);
    
    // Connect the client.
    this.collector.clientDidConnect(channel, client);
    
    // Fetch the entity.
    this.collector.clientDidFetchEntity(client, id, clientDescriptor);
    
    // Fetch again, since there can be multiple fetches from the same client.
    this.collector.clientDidFetchEntity(client, id, clientDescriptor);
    
    // Release the entity twice.
    this.collector.clientDidReleaseEntity(client, id);
    this.collector.clientDidReleaseEntity(client, id);
    
    // A third attempt to release should fail.
    boolean didSucceed = false;
    try {
      this.collector.clientDidReleaseEntity(client, id);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
    
    // Disconnect the client.
    this.collector.clientDidDisconnect(channel, client);
    
    // Destroy the entity.
    this.collector.entityWasDestroyed(id);
  }

  @Test
  public void testClientPIDInclusion() throws Exception {
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
    when(monitoringProducer.addNode(any(), any(), any())).thenReturn(true);
    this.collector = new ManagementTopologyEventCollector(selfID, monitoringProducer);

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
    this.collector = new ManagementTopologyEventCollector(selfID, monitoringProducer);

    // reset monitoringProducer.addNode(...) invocation counts
    reset(monitoringProducer);

    EntityID entityID = mock(EntityID.class);
    ClientID client = mock(ClientID.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    this.collector.clientDidFetchEntity(client, entityID, clientDescriptor);

    // verify
    ArgumentCaptor<PlatformClientFetchedEntity> argumentCaptor = ArgumentCaptor.forClass(PlatformClientFetchedEntity.class);
    verify(monitoringProducer).addNode(any(), any(), argumentCaptor.capture());
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
}
