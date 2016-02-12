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
package com.tc.objectserver.handler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.config.HaConfig;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.HydrateContext;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.core.api.ITopologyEventCollector;
import com.tc.objectserver.core.api.ServerConfigurationContext;


public class ChannelLifeCycleHandlerTest {
  private ChannelLifeCycleHandler handler;
  private ITopologyEventCollector eventCollector;


  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    CommunicationsManager commsManager = mock(CommunicationsManager.class);
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    HaConfig haConfig = mock(HaConfig.class);
    this.eventCollector = mock(ITopologyEventCollector.class);
    this.handler = new ChannelLifeCycleHandler(commsManager, channelManager, haConfig, this.eventCollector);
    ServerConfigurationContext context = mock(ServerConfigurationContext.class);
    Stage<HydrateContext> stage = mock(Stage.class);
    when(stage.getSink()).thenReturn(mock(Sink.class));
    when(context.getStage(any(String.class), (Class<HydrateContext>)any(Class.class))).thenReturn(stage);
    this.handler.initialize(context);
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
    this.handler.channelRemoved(fakeChannel);
    // We expect NOT to receive the disconnect event in the event collector.
    verify(this.eventCollector, never()).clientDidDisconnect(any(MessageChannel.class), any(ClientID.class));
  }
}
