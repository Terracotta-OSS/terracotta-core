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

package com.tc.services;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;

import com.tc.entity.ServerEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ManagedEntity;
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
  private EntityResponse response;
  private EntityID entityID;
  private ClientInstanceID clientInstanceID;
  private long version;
  private long consumerID;
  private ManagedEntity owningEntity;
  private MessageCodec<EntityMessage, EntityResponse> codec;
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
    response = mock(EntityResponse.class);
    entityID = new EntityID("foo", "bar");
    clientInstanceID = new ClientInstanceID(1);
    version = 1;
    consumerID = 1;
    owningEntity = mock(ManagedEntity.class);
    codec = mock(MessageCodec.class);
    when(codec.serialize(response)).thenReturn(payload);
    when((MessageCodec)owningEntity.getCodec()).thenReturn(codec);
    
    entityDescriptor = new EntityDescriptor(entityID, clientInstanceID, version);
    clientDescriptor = new ClientDescriptorImpl(clientID, entityDescriptor);

    communicatorService = new CommunicatorService(dsoChannelManager);
    communicatorService.channelCreated(messageChannel);
  }

  @Test
  public void testSimpleSendNoResponse() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(clientDescriptor, response);

    verify(serverEntityMessage).setMessage(entityDescriptor, payload);
    verify(serverEntityMessage).send();
  }

  @Test
  public void testSendWaitForResponse() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(clientDescriptor, response);

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
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(clientDescriptor, response);

    communicatorService.channelRemoved(messageChannel);
    future.get();
  }

  @Test
  public void testSendToDisconnectedClient() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    Future<Void> future = clientCommunicator.send(new ClientDescriptorImpl(new ClientID(2), entityDescriptor), response);
    future.get();

    verify(serverEntityMessage, never()).send();
  }

  @Test
  public void testSendNoResponseToDisconnectedClient() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(new ClientDescriptorImpl(new ClientID(2), entityDescriptor), response);

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
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);
    ServerEntityMessage serverEntityMessage1 = mock(ServerEntityMessage.class);
    ServerEntityMessage serverEntityMessage2 = mock(ServerEntityMessage.class);
    
    when(codec.serialize(response1)).thenReturn(payload1);
    when(codec.serialize(response2)).thenReturn(payload2);
    
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    
    // Send the message to client one and ensure that the correct payload went through to the correct entity.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage1);
    clientCommunicator.sendNoResponse(client1, response1);
    verify(serverEntityMessage1).setMessage(entity1, payload1);
    verify(serverEntityMessage1, never()).setMessage(eq(entity2), any(byte[].class));
    verify(serverEntityMessage1).send();
    
    // Now, the same with client two.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage2);
    clientCommunicator.sendNoResponse(client2, response2);
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
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);
    ServerEntityMessage serverEntityMessage1 = mock(ServerEntityMessage.class);
    ServerEntityMessage serverEntityMessage2 = mock(ServerEntityMessage.class);
    
    when(codec.serialize(response1)).thenReturn(payload1);
    when(codec.serialize(response2)).thenReturn(payload2);
    
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    
    // Send the message to client one and ensure that the correct payload went through to the correct entity.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage1);
    clientCommunicator.send(client1, response1);
    verify(serverEntityMessage1).setMessage(eq(entity1), eq(payload1), anyLong());
    verify(serverEntityMessage1, never()).setMessage(eq(entity2), any(byte[].class));
    verify(serverEntityMessage1).send();
    
    // Now, the same with client two.
    when(messageChannel.createMessage(TCMessageType.SERVER_ENTITY_MESSAGE)).thenReturn(serverEntityMessage2);
    clientCommunicator.send(client2, response2);
    verify(serverEntityMessage2).setMessage(eq(entity2), eq(payload2), anyLong());
    verify(serverEntityMessage2, never()).setMessage(eq(entity1), any(byte[].class));
    verify(serverEntityMessage2).send();
  }
}
