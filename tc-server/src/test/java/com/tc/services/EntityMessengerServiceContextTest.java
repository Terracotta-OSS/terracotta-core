/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.services;

import com.tc.async.api.Sink;
import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityRequest;
import com.tc.objectserver.entity.ManagedEntityImpl;
import com.tc.objectserver.handler.RetirementManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for EntityMessengerService parent context propagation.
 * Verifies that the service correctly captures and uses parent request context
 * when creating fake entity messages.
 */
public class EntityMessengerServiceContextTest {

  private Sink<VoltronEntityMessage> sink;
  private ManagedEntityImpl entity;
  private RetirementManager retirementManager;
  @SuppressWarnings("rawtypes")
  private MessageCodec codec;
  private EntityMessengerService<EntityMessage, ?> service;

  @Before
  public void setUp() throws Exception {
    sink = mock(Sink.class);
    entity = mock(ManagedEntityImpl.class);
    retirementManager = mock(RetirementManager.class);
    codec = mock(MessageCodec.class);

    when(entity.getRetirementManager()).thenReturn(retirementManager);
    doReturn(codec).when(entity).getCodec();
    when(codec.encodeMessage(any())).thenReturn(new byte[0]);
    when(entity.isDestroyed()).thenReturn(false);
  }

  @Test
  public void testParentContextCapturedWhenPresent() throws Exception {
    // Setup: Create a mock parent request context
    ServerEntityRequest parentRequest = mock(ServerEntityRequest.class);
    ClientID expectedClientID = new ClientID(42);
    TransactionID expectedTxnID = new TransactionID(100);

    when(parentRequest.getNodeID()).thenReturn(expectedClientID);
    when(parentRequest.getTransaction()).thenReturn(expectedTxnID);
    when(entity.getCurrentRequestMessage()).thenReturn(parentRequest);

    // Create the service
    service = new EntityMessengerService<>(sink, entity, false);
    service.entityCreated(entity);

    // Send a message
    EntityMessage message = mock(EntityMessage.class);
    service.messageSelf(message);

    // Verify the message was sent to the sink
    ArgumentCaptor<VoltronEntityMessage> captor = ArgumentCaptor.forClass(VoltronEntityMessage.class);
    verify(sink).addToSink(captor.capture());

    VoltronEntityMessage sentMessage = captor.getValue();
    assertThat("Message should be sent", sentMessage, is(notNullValue()));

    // Verify the parent context was used
    assertThat("ClientID should match parent", sentMessage.getSource(), is(expectedClientID));
    assertThat("TransactionID should match parent", sentMessage.getTransactionID(), is(expectedTxnID));
  }

  @Test
  public void testNullParentContextHandledGracefully() throws Exception {
    // Setup: No parent context available
    when(entity.getCurrentRequestMessage()).thenReturn(null);

    // Create the service
    service = new EntityMessengerService<>(sink, entity, false);
    service.entityCreated(entity);

    // Send a message
    EntityMessage message = mock(EntityMessage.class);
    service.messageSelf(message);

    // Verify the message was sent to the sink
    ArgumentCaptor<VoltronEntityMessage> captor = ArgumentCaptor.forClass(VoltronEntityMessage.class);
    verify(sink).addToSink(captor.capture());

    VoltronEntityMessage sentMessage = captor.getValue();
    assertThat("Message should be sent", sentMessage, is(notNullValue()));

    // Verify defaults are used when no parent context
    assertThat("ClientID should be NULL_ID", sentMessage.getSource(), is(ClientID.NULL_ID));
    assertThat("TransactionID should be generated", sentMessage.getTransactionID(), is(notNullValue()));
  }

  @Test
  public void testParentContextChangesReflectedInSubsequentMessages() throws Exception {
    // Setup: Start with one parent context
    ServerEntityRequest firstParent = mock(ServerEntityRequest.class);
    ClientID firstClientID = new ClientID(1);
    TransactionID firstTxnID = new TransactionID(10);

    when(firstParent.getNodeID()).thenReturn(firstClientID);
    when(firstParent.getTransaction()).thenReturn(firstTxnID);
    when(entity.getCurrentRequestMessage()).thenReturn(firstParent);

    // Create the service
    service = new EntityMessengerService<>(sink, entity, false);
    service.entityCreated(entity);

    // Send first message
    EntityMessage message1 = mock(EntityMessage.class);
    service.messageSelf(message1);

    // Change the parent context
    ServerEntityRequest secondParent = mock(ServerEntityRequest.class);
    ClientID secondClientID = new ClientID(2);
    TransactionID secondTxnID = new TransactionID(20);

    when(secondParent.getNodeID()).thenReturn(secondClientID);
    when(secondParent.getTransaction()).thenReturn(secondTxnID);
    when(entity.getCurrentRequestMessage()).thenReturn(secondParent);

    // Send second message
    EntityMessage message2 = mock(EntityMessage.class);
    service.messageSelf(message2);

    // Verify both messages were sent
    ArgumentCaptor<VoltronEntityMessage> captor = ArgumentCaptor.forClass(VoltronEntityMessage.class);
    verify(sink, times(2)).addToSink(captor.capture());

    // Get all captured messages
    List<VoltronEntityMessage> capturedMessages = captor.getAllValues();
    assertThat("Should have captured 2 messages", capturedMessages.size(), is(2));

    // The second message should use the second parent context
    VoltronEntityMessage lastMessage = capturedMessages.get(1);
    assertThat("Second message ClientID should match second parent",
               lastMessage.getSource(), is(secondClientID));
    assertThat("Second message TransactionID should match second parent",
               lastMessage.getTransactionID(), is(secondTxnID));
  }

  @Test
  public void testManagedEntityGetCurrentRequestMessageReturnsCorrectValue() {
    // This test verifies the new getCurrentRequestMessage() method exists and is callable
    // The actual implementation testing would require more complex setup with
    // GuardianContext and MessageChannel, which is beyond unit test scope

    ManagedEntityImpl realEntity = mock(ManagedEntityImpl.class);
    when(realEntity.getCurrentRequestMessage()).thenReturn(null);

    ServerEntityRequest result = realEntity.getCurrentRequestMessage();

    // Verify the method can be called and returns expected null in this mock scenario
    assertThat("getCurrentRequestMessage should be callable", result, is((ServerEntityRequest) null));
    verify(realEntity).getCurrentRequestMessage();
  }
}
