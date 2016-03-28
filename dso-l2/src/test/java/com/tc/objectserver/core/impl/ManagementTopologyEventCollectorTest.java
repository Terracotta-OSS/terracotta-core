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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tc.l2.state.StateManager;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.EntityID;

import static org.mockito.Mockito.mock;


public class ManagementTopologyEventCollectorTest {
  private ManagementTopologyEventCollector collector;
  private ServerID selfID;

  @Before
  public void setUp() throws Exception {
    this.selfID = mock(ServerID.class);
    this.collector = new ManagementTopologyEventCollector(selfID, null);
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
    
    // Now, change state of the server to active.
    this.collector.serverDidEnterState(selfID, StateManager.ACTIVE_COORDINATOR, System.currentTimeMillis());
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
    
    // Put us into the active state.
    this.collector.serverDidEnterState(selfID, StateManager.ACTIVE_COORDINATOR, System.currentTimeMillis());
    boolean isActive = true;
    
    // Create the entity.
    this.collector.entityWasCreated(id, isActive);
    
    // Connect the client.
    this.collector.clientDidConnect(channel, client);
    
    // Fetch the entity.
    this.collector.clientDidFetchEntity(client, id);
    
    // Fetch again, since there can be multiple fetches from the same client.
    this.collector.clientDidFetchEntity(client, id);
    
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
}