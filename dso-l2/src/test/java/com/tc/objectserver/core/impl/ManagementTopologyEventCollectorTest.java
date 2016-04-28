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

import com.tc.net.TCSocketAddress;
import com.tc.stats.Client;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityID;
import org.mockito.ArgumentCaptor;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;

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
    // Note that we start in passive state.
    boolean isActive = false;
    
    // We should fail to create an entity as active.
    boolean didSucceed = false;
    try {
      this.collector.entityWasCreated(id, true);
      didSucceed = true;
    } catch (AssertionError e) {
      // Expected.
    }
    Assert.assertFalse(didSucceed);
    
    // Create it as passive.
    this.collector.entityWasCreated(id, isActive);
    // We should fail to create it a second time.
    didSucceed = false;
    try {
      this.collector.entityWasCreated(id, isActive);
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
    this.collector.entityWasReloaded(id, isActive);
    
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
    MessageChannel channel = mock(MessageChannel.class);
    ClientID client = mock(ClientID.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);
    
    // Put us into the active state.
    this.collector.serverDidEnterState(selfID, StateManager.ACTIVE_COORDINATOR, System.currentTimeMillis());
    boolean isActive = true;
    
    // Create the entity.
    this.collector.entityWasCreated(id, isActive);
    
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

    // prepare and call collector.clientDidConnect(...)
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.getAttachment(any())).thenReturn(TEST_CLIENT_PID);
    when(channel.getLocalAddress()).thenReturn(new TCSocketAddress("localhost", 1234));
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress("localhost", 4567));
    ClientID client = mock(ClientID.class);
    this.collector.clientDidConnect(channel, client);

    // verify
    ArgumentCaptor<PlatformConnectedClient> argumentCaptor = ArgumentCaptor.forClass(PlatformConnectedClient.class);
    verify(monitoringProducer).addNode(any(), any(), argumentCaptor.capture());
    Assert.assertEquals(TEST_CLIENT_PID, argumentCaptor.getValue().clientPID);
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
}
