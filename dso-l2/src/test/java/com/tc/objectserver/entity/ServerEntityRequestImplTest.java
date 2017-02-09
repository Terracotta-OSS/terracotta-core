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
package com.tc.objectserver.entity;

import org.junit.Before;
import org.junit.Test;

import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.entity.VoltronEntityRetiredResponse;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.FetchID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ServerEntityRequestImplTest {
  private MessageChannel messageChannel;
  private VoltronEntityReceivedResponse requestAckMessage;
  private VoltronEntityAppliedResponse responseMessage;
  private VoltronEntityRetiredResponse retiredMessage;
  private EntityDescriptor entityDescriptor;
  private TransactionID transactionID;
  private ClientID nodeID;

  @Before
  public void setUp() throws Exception {
    requestAckMessage = mock(VoltronEntityReceivedResponse.class);
    responseMessage = mock(VoltronEntityAppliedResponse.class);
    retiredMessage = mock(VoltronEntityRetiredResponse.class);
    messageChannel = mockMessageChannel(requestAckMessage, responseMessage, retiredMessage);
    entityDescriptor = EntityDescriptor.createDescriptorForLifecycle(new EntityID("foo", "bar"), 1);
    transactionID = new TransactionID(1);
    nodeID = mock(ClientID.class);
  }

  @Test
  public void testCompleteInvoke() throws Exception {
    ServerEntityRequestResponse serverEntityRequest = buildInvoke();
    
    byte[] value = new byte[0];
    serverEntityRequest.complete(value);
    serverEntityRequest.retired();
    
    verify(responseMessage).setSuccess(transactionID, value, false);
    verify(responseMessage).send();
  }

  @Test
  public void testCompleteCreate() throws Exception {
    boolean requiresReplication = true;
    boolean isReplicatedMessage = false;
    ServerEntityRequestResponse serverEntityRequest = new ServerEntityRequestResponse(entityDescriptor, ServerEntityAction.CREATE_ENTITY, transactionID, TransactionID.NULL_ID, nodeID, ()->Optional.of(messageChannel), requiresReplication, isReplicatedMessage);

    serverEntityRequest.complete();
    serverEntityRequest.retired();
    
    verify(responseMessage).setSuccess(transactionID, new byte[0], false);
    verify(responseMessage).send();
  }

  @Test
  public void testRequestedAcks() throws Exception {
    ServerEntityRequestResponse serverEntityRequest = buildInvoke();
    
    verify(requestAckMessage, never()).send();
    serverEntityRequest.received();
    
    verify(requestAckMessage).setTransactionID(transactionID);
    verify(requestAckMessage).send();
  }

  private static MessageChannel mockMessageChannel(VoltronEntityReceivedResponse requestAckMessage, VoltronEntityAppliedResponse responseMessage, VoltronEntityRetiredResponse retiredMessage) {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(requestAckMessage);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE)).thenReturn(responseMessage);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE)).thenReturn(retiredMessage);
    return channel;
  }

  private ServerEntityRequestResponse buildInvoke() {
    boolean isReplicatedMessage = false;
    boolean isReceivedRequested = false;
    EntityDescriptor.createDescriptorForInvoke(new FetchID(1L), new ClientInstanceID(1));
    return new ServerEntityRequestResponse(entityDescriptor, ServerEntityAction.INVOKE_ACTION, transactionID, TransactionID.NULL_ID, nodeID, ()->Optional.of(messageChannel), isReceivedRequested, isReplicatedMessage);
  }
}
