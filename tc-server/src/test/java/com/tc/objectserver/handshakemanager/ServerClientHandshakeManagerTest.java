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
package com.tc.objectserver.handshakemanager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.ServerMode;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.productinfo.ProductInfo;


public class ServerClientHandshakeManagerTest {
  private DSOChannelManager channelManager;
  private EntityManager entityManager;
  private ProcessTransactionHandler transactionHandler;
  private ServerClientHandshakeManager manager;
  private Stage voltronStage;
  private Sink voltronSink;
  private Timer timer = mock(Timer.class);

  @Before
  public void setUp() throws Exception {
    Logger logger = mock(Logger.class);
    this.channelManager = mock(DSOChannelManager.class);
    this.transactionHandler = mock(ProcessTransactionHandler.class);
    StageManager stageManager = mock(StageManager.class);
    timer = mock(Timer.class);
    Supplier<Long> reconnectTimeoutSupplier = () -> 1000L;
    Logger consoleLogger = mock(Logger.class);
    voltronStage = mock(Stage.class);
    voltronSink = mock(Sink.class);
    when(voltronStage.getSink()).thenReturn(voltronSink);
    when(stageManager.getStage(any(), any())).thenReturn(voltronStage);
    ConsistencyManager consistency = mock(ConsistencyManager.class);
    when(consistency.requestTransition(any(ServerMode.class), any(NodeID.class), any(ConsistencyManager.Transition.class))).thenReturn(Boolean.TRUE);
    this.manager = new ServerClientHandshakeManager(logger, consistency, this.channelManager, timer, reconnectTimeoutSupplier, voltronSink, ProductInfo.getInstance(), consoleLogger);
  }

  @Test
  public void testStartNoExisting() throws Exception {
    assertFalse(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    Set<ClientID> existingConnections = Collections.emptySet();
    this.manager.setStarting(existingConnections);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
  }

  @Test
  public void testStartOneExisting() throws Exception {
    assertFalse(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    Set<ClientID> existingConnections = Collections.singleton(mock(ClientID.class));
    this.manager.setStarting(existingConnections);
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
  }

  @Test
  public void testReconnectNoData() throws Exception {
    ClientID connection = mock(ClientID.class);
    Set<ClientID> existingConnections = Collections.singleton(connection);
    this.manager.setStarting(existingConnections);
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see the server start after it gets this handshake since it was waiting for one connection.
    ClientHandshakeMessage handshake = mock(ClientHandshakeMessage.class);
    when(handshake.getClientVersion()).thenReturn("");
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(handshake.getChannel()).thenReturn(messageChannel);
    when(handshake.getSourceNodeID()).thenReturn(connection);
    this.manager.notifyClientConnect(handshake, entityManager, transactionHandler);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    
    verify(this.voltronSink).addToSink(any(LocalPipelineFlushMessage.class));
  }

  @Test
  public void testTwoReconnectWithData() throws Exception {
    // Note that we need to ensure that we hook together all these objects such that the ConnectionID we are waiting
    // for refers to the same source node as the corresponding message.
    ClientID client1 = new ClientID(1);
    ConnectionID connection1 = mock(ConnectionID.class);
    when(connection1.getChannelID()).thenReturn(1L);
    when(this.channelManager.getClientIDFor(new ChannelID(1))).thenReturn(client1);
    
    ClientID client2 = new ClientID(2);
    ConnectionID connection2 = mock(ConnectionID.class);
    when(connection2.getChannelID()).thenReturn(2L);
    when(this.channelManager.getClientIDFor(new ChannelID(2))).thenReturn(client2);
    
    Set<ClientID> existingConnections = new HashSet<>();
    existingConnections.add(client1);
    existingConnections.add(client2);
    this.manager.setStarting(existingConnections);
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see this first message go through to the transaction handler but not start the server.
    ClientHandshakeMessage message1 = mock(ClientHandshakeMessage.class);
    when(message1.getClientVersion()).thenReturn("");
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel1 = mock(MessageChannel.class);
    when(message1.getChannel()).thenReturn(messageChannel1);
    ResendVoltronEntityMessage resend = mock(ResendVoltronEntityMessage.class);
    when(message1.getSourceNodeID()).thenReturn(client1);
    when(message1.getResendMessages()).thenReturn(Collections.singleton(resend));
    this.manager.notifyClientConnect(message1, entityManager, transactionHandler);
    assertFalse(this.manager.isStarted());
    verify(this.transactionHandler).handleResentMessage(resend);
    verify(this.voltronSink, never()).addToSink(any(LocalPipelineFlushMessage.class));
    
    // This second message will now start the server.
    ClientHandshakeMessage message2 = mock(ClientHandshakeMessage.class);
    when(message2.getClientVersion()).thenReturn("");
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel2 = mock(MessageChannel.class);
    when(message2.getChannel()).thenReturn(messageChannel2);
    when(message2.getSourceNodeID()).thenReturn(client2);
    this.manager.notifyClientConnect(message2, entityManager, transactionHandler);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    verify(this.voltronSink).addToSink(any(LocalPipelineFlushMessage.class));
  }

  @Test
  public void testFailedReconnects() throws Exception {
    ClientID client1 = new ClientID(1);
    ConnectionID connection1 = mock(ConnectionID.class);
    when(connection1.getChannelID()).thenReturn(1L);
    when(this.channelManager.getClientIDFor(new ChannelID(1))).thenReturn(client1);

    Set<ClientID> existingConnections = new HashSet<>();
    existingConnections.add(client1);
    this.manager.setStarting(existingConnections);
    this.manager.notifyTimeout();
    assertTrue(this.manager.getUnconnectedClients().isEmpty());
  }

  @Test
  public void testNoTimer() throws Exception {
    ClientID client1 = new ClientID(1);
    ConnectionID connection1 = mock(ConnectionID.class);
    when(connection1.getChannelID()).thenReturn(1L);
    when(this.channelManager.getClientIDFor(new ChannelID(1))).thenReturn(client1);

    Set<ClientID> existingConnections = new HashSet<>();
    existingConnections.add(client1);
    this.manager.setStarting(existingConnections);
    ClientHandshakeMessage handshake = mock(ClientHandshakeMessage.class);
    when(handshake.getSourceNodeID()).thenReturn(client1);
    when(handshake.getChannel()).thenReturn(mock(MessageChannel.class));
    when(handshake.getClientAddress()).thenReturn("127.0.0.1");
    when(handshake.getClientVersion()).thenReturn("1.0");
    when(handshake.getName()).thenReturn("test");
    when(handshake.getUUID()).thenReturn("1234567890");
    when(this.channelManager.getClientIDFor(new ChannelID(1))).thenReturn(client1);
    this.manager.notifyClientConnect(handshake, entityManager, transactionHandler);
    assertTrue(this.manager.isStarted());
    assertTrue(this.manager.getUnconnectedClients().isEmpty());
    verify(timer).cancel();
  }
}