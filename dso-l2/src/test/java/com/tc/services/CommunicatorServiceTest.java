package com.tc.services;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;

import com.tc.entity.ServerEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.entity.ClientDescriptorImpl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author twu
 */
public class CommunicatorServiceTest {
  private CommunicatorService communicatorService;
  private DSOChannelManager dsoChannelManager;
  private ClientID clientID;
  private MessageChannel messageChannel;
  private ServerEntityMessage serverEntityMessage;
  private byte[] payload;
  private EntityID entityID;
  private ClientInstanceID clientInstanceID;
  private long version;
  private long consumerID;
  private ClientDescriptor clientDescriptor;
  private EntityDescriptor entityDescriptor;

  @Before
  public void setUp() throws Exception {
    clientID = new ClientID(1);
    messageChannel = mock(MessageChannel.class);
    when(messageChannel.getRemoteNodeID()).thenReturn(clientID);
    serverEntityMessage = mock(ServerEntityMessage.class);
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage);
    dsoChannelManager = mock(DSOChannelManager.class);
    when(dsoChannelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    payload = new byte[1];
    entityID = new EntityID("foo", "bar");
    clientInstanceID = new ClientInstanceID(1);
    version = 1;
    consumerID = 1;
    entityDescriptor = new EntityDescriptor(entityID, clientInstanceID, version);
    clientDescriptor = new ClientDescriptorImpl(clientID, entityDescriptor);

    communicatorService = new CommunicatorService(dsoChannelManager);
    communicatorService.channelCreated(messageChannel);
  }

  @Test
  public void testSimpleSendNoResponse() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(clientDescriptor, payload);

    verify(serverEntityMessage).setMessage(entityDescriptor, payload);
    verify(serverEntityMessage).send();
  }

  @Test
  public void testSendWaitForResponse() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(clientDescriptor, payload);

    verify(serverEntityMessage).setMessage(entityDescriptor, payload, 0L);
    verify(serverEntityMessage).send();

    try {
      future.get(1, TimeUnit.SECONDS);
      fail("Should have timed out");
    } catch (TimeoutException e) {
      // expected
    }

    communicatorService.response(clientID, 0L);
    future.get();
  }

  @Test
  public void testClientLeavesWhileResponseOutstanding() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(clientDescriptor, payload);

    communicatorService.channelRemoved(messageChannel);
    future.get();
  }

  @Test
  public void testSendToDisconnectedClient() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(new ClientDescriptorImpl(new ClientID(2), entityDescriptor), payload);
    future.get();

    verify(serverEntityMessage, never()).send();
  }

  @Test
  public void testSendNoResponseToDisconnectedClient() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(new ClientDescriptorImpl(new ClientID(2), entityDescriptor), payload);

    verify(serverEntityMessage, never()).send();
  }

  @Test
  public void testSendSpecificTargetNoResponse() throws Exception {
    // We need to create explicit instances for this test since we are working with specific ones.
    ClientInstanceID instance1 = new ClientInstanceID(1);
    ClientInstanceID instance2 = new ClientInstanceID(2);
    EntityDescriptor entity1 = new EntityDescriptor(entityID, instance1, version);
    EntityDescriptor entity2 = new EntityDescriptor(entityID, instance2, version);
    ClientDescriptor client1 = new ClientDescriptorImpl(clientID, entity1);
    ClientDescriptor client2 = new ClientDescriptorImpl(clientID, entity2);
    byte[] payload1 = new byte[1];
    byte[] payload2 = new byte[2];
    ServerEntityMessage serverEntityMessage1 = mock(ServerEntityMessage.class);
    ServerEntityMessage serverEntityMessage2 = mock(ServerEntityMessage.class);
    
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    
    // Send the message to client one and ensure that the correct payload went through to the correct entity.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage1);
    clientCommunicator.sendNoResponse(client1, payload1);
    verify(serverEntityMessage1).setMessage(entity1, payload1);
    verify(serverEntityMessage1, never()).setMessage(eq(entity2), any(byte[].class));
    verify(serverEntityMessage1).send();
    
    // Now, the same with client two.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage2);
    clientCommunicator.sendNoResponse(client2, payload2);
    verify(serverEntityMessage2).setMessage(entity2, payload2);
    verify(serverEntityMessage2, never()).setMessage(eq(entity1), any(byte[].class));
    verify(serverEntityMessage2).send();
  }

  @Test
  public void testSendSpecificTargetWithResponse() throws Exception {
    // We need to create explicit instances for this test since we are working with specific ones.
    ClientInstanceID instance1 = new ClientInstanceID(1);
    ClientInstanceID instance2 = new ClientInstanceID(2);
    EntityDescriptor entity1 = new EntityDescriptor(entityID, instance1, version);
    EntityDescriptor entity2 = new EntityDescriptor(entityID, instance2, version);
    ClientDescriptor client1 = new ClientDescriptorImpl(clientID, entity1);
    ClientDescriptor client2 = new ClientDescriptorImpl(clientID, entity2);
    byte[] payload1 = new byte[1];
    byte[] payload2 = new byte[2];
    ServerEntityMessage serverEntityMessage1 = mock(ServerEntityMessage.class);
    ServerEntityMessage serverEntityMessage2 = mock(ServerEntityMessage.class);
    
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, new CommunicatorServiceConfiguration());
    
    // Send the message to client one and ensure that the correct payload went through to the correct entity.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage1);
    clientCommunicator.send(client1, payload1);
    verify(serverEntityMessage1).setMessage(eq(entity1), eq(payload1), anyLong());
    verify(serverEntityMessage1, never()).setMessage(eq(entity2), any(byte[].class));
    verify(serverEntityMessage1).send();
    
    // Now, the same with client two.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage2);
    clientCommunicator.send(client2, payload2);
    verify(serverEntityMessage2).setMessage(eq(entity2), eq(payload2), anyLong());
    verify(serverEntityMessage2, never()).setMessage(eq(entity1), any(byte[].class));
    verify(serverEntityMessage2).send();
  }
}
