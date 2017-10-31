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
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ManagedEntity;
import com.tc.objectserver.entity.ClientDescriptorImpl;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.fail;
import org.mockito.Mockito;
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
  private ClientInstanceID clientInstanceID;
  private long consumerID;
  private ManagedEntity owningEntity;
  private MessageCodec<EntityMessage, EntityResponse> codec;
  private ClientDescriptor clientDescriptor;
  private ClientMessageSender clientMessageSender;

  @Before
  public void setUp() throws Exception {
    clientID = new ClientID(1);
    messageChannel = mock(MessageChannel.class);
    when(messageChannel.getRemoteNodeID()).thenReturn(clientID);
    serverEntityMessage = mock(ServerEntityMessage.class);
    dsoChannelManager = mock(DSOChannelManager.class);
    when(dsoChannelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    payload = new byte[1];
    response = mock(EntityResponse.class);
    clientInstanceID = new ClientInstanceID(1);
    consumerID = 1;
    owningEntity = mock(ManagedEntity.class);
    codec = mock(MessageCodec.class);
    when(codec.encodeResponse(response)).thenReturn(payload);
    when((MessageCodec)owningEntity.getCodec()).thenReturn(codec);
    
    clientDescriptor = new ClientDescriptorImpl(clientID, clientInstanceID);

    clientMessageSender = mock(ClientMessageSender.class);
    communicatorService = new CommunicatorService(clientMessageSender);
    dsoChannelManager.addEventListener(communicatorService);
    communicatorService.initialized();

    // Note that we can only serve this service if in active mode.
    communicatorService.serverDidBecomeActive();
    communicatorService.channelCreated(messageChannel);
  }

  @Test
  public void testSimpleSendNoResponse() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(clientDescriptor, response);

    verify(clientMessageSender).send(clientID, clientInstanceID, payload);
  }

  @Test
  public void testSendNoResponseToDisconnectedClient() throws Exception {
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    clientCommunicator.sendNoResponse(new ClientDescriptorImpl(new ClientID(2), clientInstanceID), response);

    verify(serverEntityMessage, never()).send();
  }

  @Test
  public void testSendSpecificTargetNoResponse() throws Exception {
    // We need to create explicit instances for this test since we are working with specific ones.
    ClientInstanceID instance1 = new ClientInstanceID(1);
    ClientInstanceID instance2 = new ClientInstanceID(2);
    ClientDescriptor client1 = new ClientDescriptorImpl(clientID, instance1);
    ClientDescriptor client2 = new ClientDescriptorImpl(clientID, instance2);
    byte[] payload1 = new byte[1];
    byte[] payload2 = new byte[2];
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);
    
    when(codec.encodeResponse(response1)).thenReturn(payload1);
    when(codec.encodeResponse(response2)).thenReturn(payload2);
    
    ClientCommunicator clientCommunicator = communicatorService.getService(consumerID, this.owningEntity, new CommunicatorServiceConfiguration());
    
    clientCommunicator.sendNoResponse(client1, response1);
    verify(clientMessageSender).send(clientID, instance1, payload1);
    verify(clientMessageSender, never()).send(eq(clientID), eq(instance2), any(byte[].class));
    
    // Now, the same with client two.
    Mockito.reset(clientMessageSender);
    clientCommunicator.sendNoResponse(client2, response2);
    verify(clientMessageSender).send(clientID, instance2, payload2);
    verify(clientMessageSender, never()).send(eq(clientID), eq(instance1), any(byte[].class));
  }
}
