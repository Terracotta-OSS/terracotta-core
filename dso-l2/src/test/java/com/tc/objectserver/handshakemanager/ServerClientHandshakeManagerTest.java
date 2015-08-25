package com.tc.objectserver.handshakemanager;

import org.junit.Before;
import org.junit.Test;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.locks.LockManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ServerClientHandshakeManagerTest {
  private DSOChannelManager channelManager;
  private LockManager lockManager;
  private ProcessTransactionHandler transactionHandler;
  private ServerClientHandshakeManager manager;

  @Before
  public void setUp() throws Exception {
    TCLogger logger = mock(TCLogger.class);
    this.channelManager = mock(DSOChannelManager.class);
    this.lockManager = mock(LockManager.class);
    EntityManager entityManager = mock(EntityManager.class);
    this.transactionHandler = mock(ProcessTransactionHandler.class);
    Timer timer = mock(Timer.class);
    long reconnectTimeout = 1000;
    boolean persistent = true;
    TCLogger consoleLogger = mock(TCLogger.class);
    this.manager = new ServerClientHandshakeManager(logger, this.channelManager, this.lockManager, entityManager, this.transactionHandler, timer, reconnectTimeout, persistent, consoleLogger);
  }

  @Test
  public void testStartNoExisting() throws Exception {
    assertFalse(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    Set<ConnectionID> existingConnections = Collections.emptySet();
    this.manager.setStarting(existingConnections);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
  }

  @Test
  public void testStartOneExisting() throws Exception {
    assertFalse(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    Set<ConnectionID> existingConnections = Collections.singleton(mock(ConnectionID.class));
    this.manager.setStarting(existingConnections);
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
  }

  @Test
  public void testReconnectNoData() throws Exception {
    ConnectionID connection = mock(ConnectionID.class);
    Set<ConnectionID> existingConnections = Collections.singleton(connection);
    this.manager.setStarting(existingConnections);
    this.manager.startReconnectWindow();
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see the server start after it gets this handshake since it was waiting for one connection.
    ClientHandshakeMessage message = mock(ClientHandshakeMessage.class);
    this.manager.notifyClientConnect(message);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    
    verify(this.lockManager).start();
    verify(this.transactionHandler).executeAllResends();
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
    
    Set<ConnectionID> existingConnections = new HashSet<>();
    existingConnections.add(connection1);
    existingConnections.add(connection2);
    this.manager.setStarting(existingConnections);
    this.manager.startReconnectWindow();
    assertTrue(this.manager.isStarting());
    assertFalse(this.manager.isStarted());
    
    // We should see this first message go through to the transaction handler but not start the server.
    ClientHandshakeMessage message1 = mock(ClientHandshakeMessage.class);
    ResendVoltronEntityMessage resend = mock(ResendVoltronEntityMessage.class);
    when(message1.getSourceNodeID()).thenReturn(client1);
    when(message1.getResendMessages()).thenReturn(Collections.singleton(resend));
    this.manager.notifyClientConnect(message1);
    assertFalse(this.manager.isStarted());
    verify(this.lockManager, never()).start();
    verify(this.transactionHandler).handleResentMessage(resend);
    verify(this.transactionHandler, never()).executeAllResends();
    
    // This second message will now start the server.
    ClientHandshakeMessage message2 = mock(ClientHandshakeMessage.class);
    when(message2.getSourceNodeID()).thenReturn(client2);
    this.manager.notifyClientConnect(message2);
    assertFalse(this.manager.isStarting());
    assertTrue(this.manager.isStarted());
    verify(this.lockManager).start();
    verify(this.transactionHandler).executeAllResends();
  }
}