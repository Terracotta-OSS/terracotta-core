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
package com.tc.objectserver.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.entity.LocalPipelineFlushMessage;
import com.tc.objectserver.handler.ProcessTransactionHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ServerClientHandshakeManagerTest {
  private DSOChannelManager channelManager;
  private EntityManager entityManager;
  private ProcessTransactionHandler transactionHandler;
  private ServerClientHandshakeManager manager;
  private Stage voltronStage;
  private Sink voltronSink;

  @Before
  public void setUp() throws Exception {
    Logger logger = mock(Logger.class);
    this.channelManager = mock(DSOChannelManager.class);
    this.transactionHandler = mock(ProcessTransactionHandler.class);
    StageManager stageManager = mock(StageManager.class);
    Timer timer = mock(Timer.class);
    long reconnectTimeout = 1000;
    Logger consoleLogger = mock(Logger.class);
    voltronStage = mock(Stage.class);
    voltronSink = mock(Sink.class);
    when(voltronStage.getSink()).thenReturn(voltronSink);
    when(stageManager.getStage(any(), any())).thenReturn(voltronStage);
    this.manager = new ServerClientHandshakeManager(logger, this.channelManager, stageManager, timer, reconnectTimeout, consoleLogger);
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
    this.manager.startReconnectWindow();
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see the server start after it gets this handshake since it was waiting for one connection.
    ClientHandshakeMessage handshake = mock(ClientHandshakeMessage.class);
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(handshake.getChannel()).thenReturn(messageChannel);
    when(handshake.getSourceNodeID()).thenReturn(connection);
    this.manager.notifyClientConnect(handshake, entityManager, transactionHandler);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    
    verify(this.voltronSink).addSingleThreaded(any(LocalPipelineFlushMessage.class));
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
    this.manager.startReconnectWindow();
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see this first message go through to the transaction handler but not start the server.
    ClientHandshakeMessage message1 = mock(ClientHandshakeMessage.class);
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel1 = mock(MessageChannel.class);
    when(message1.getChannel()).thenReturn(messageChannel1);
    ResendVoltronEntityMessage resend = mock(ResendVoltronEntityMessage.class);
    when(message1.getSourceNodeID()).thenReturn(client1);
    when(message1.getResendMessages()).thenReturn(Collections.singleton(resend));
    this.manager.notifyClientConnect(message1, entityManager, transactionHandler);
    assertFalse(this.manager.isStarted());
    verify(this.transactionHandler).handleResentMessage(resend);
    verify(this.voltronSink, never()).addSingleThreaded(any(LocalPipelineFlushMessage.class));
    
    // This second message will now start the server.
    ClientHandshakeMessage message2 = mock(ClientHandshakeMessage.class);
    // We also need to provide a messageChannel since the manager will try to add an attachment to it (so it can't be null).
    MessageChannel messageChannel2 = mock(MessageChannel.class);
    when(message2.getChannel()).thenReturn(messageChannel2);
    when(message2.getSourceNodeID()).thenReturn(client2);
    this.manager.notifyClientConnect(message2, entityManager, transactionHandler);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    verify(this.voltronSink).addSingleThreaded(any(LocalPipelineFlushMessage.class));
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
}