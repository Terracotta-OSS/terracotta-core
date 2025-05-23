/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.objectserver.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.impl.ManagementTopologyEventCollector;
import com.tc.objectserver.entity.ClientEntityStateManager;
import org.terracotta.monitoring.IMonitoringProducer;


public class ClientChannelLifeCycleHandlerTest {
  private ClientChannelLifeCycleHandler handler;
  private ITopologyEventCollector eventCollector;


  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    StageManager stageManager = mock(StageManager.class);
    CommunicationsManager commsManager = mock(CommunicationsManager.class);
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    this.eventCollector = mock(ITopologyEventCollector.class);
    Stage<HydrateContext> stage = mock(Stage.class);
    when(stage.getSink()).thenReturn(mock(Sink.class));
    when(stageManager.getStage(any(String.class), (Class<HydrateContext>)any(Class.class))).thenReturn(stage);
    this.handler = new ClientChannelLifeCycleHandler(commsManager, stageManager, channelManager,
      mock(ClientEntityStateManager.class), 
      mock(ProcessTransactionHandler.class), new ManagementTopologyEventCollector(mock(IMonitoringProducer.class)));
  }

  @After
  public void tearDown() throws Exception {
    // Do nothing.
  }

  @Test
  public void testFalseDisconnect() throws Exception {
    // This test ensures that the ITopologyEventCollector will not see a disconnected event if the channel has no local
    // address.
    MessageChannel fakeChannel = mock(MessageChannel.class);
    // We will use a real remote node ID.
    when(fakeChannel.getRemoteNodeID()).thenReturn(mock(ClientID.class));
    // But a null local address.
    when(fakeChannel.getLocalAddress()).thenReturn(null);

    when(fakeChannel.getChannelID()).thenReturn(ChannelID.NULL_ID);
    
    this.handler.channelRemoved(fakeChannel);
  }
}
