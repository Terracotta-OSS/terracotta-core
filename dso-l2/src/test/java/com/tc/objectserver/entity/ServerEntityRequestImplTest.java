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
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientInstanceID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import com.tc.objectserver.api.ServerEntityRequest;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ServerEntityRequestImplTest {
  private MessageChannel messageChannel;
  private VoltronEntityReceivedResponse requestAckMessage;
  private VoltronEntityAppliedResponse responseMessage;
  private EntityDescriptor entityDescriptor;
  private TransactionID transactionID;
  private NodeID nodeID;
  private byte[] payload;

  @Before
  public void setUp() throws Exception {
    requestAckMessage = mock(VoltronEntityReceivedResponse.class);
    responseMessage = mock(VoltronEntityAppliedResponse.class);
    messageChannel = mockMessageChannel(requestAckMessage, responseMessage);
    entityDescriptor = new EntityDescriptor(new EntityID("foo", "bar"), new ClientInstanceID(1), 1);
    transactionID = new TransactionID(1);
    nodeID = mock(NodeID.class);
    payload = new byte[0];
  }

  @Test
  public void testCompleteInvoke() throws Exception {
    ServerEntityRequest serverEntityRequest = buildInvoke();
    
    byte[] value = new byte[0];
    serverEntityRequest.complete(value);
    
    verify(responseMessage).setSuccess(transactionID, value);
    verify(responseMessage).send();
  }

  @Test
  public void testCompleteCreate() throws Exception {
    boolean requiresReplication = true;
    ServerEntityRequest serverEntityRequest = new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.CREATE_ENTITY, payload, transactionID, TransactionID.NULL_ID, nodeID, requiresReplication, Optional.of(messageChannel));

    serverEntityRequest.complete();
    
    verify(responseMessage).setSuccess(transactionID, new byte[0]);
    verify(responseMessage).send();
  }

  @Test
  public void testRequestedAcks() throws Exception {
    ServerEntityRequest serverEntityRequest = buildInvoke();
    
    verify(requestAckMessage, never()).send();
    serverEntityRequest.received();
    
    verify(requestAckMessage).setTransactionID(transactionID);
    verify(requestAckMessage).send();
  }

  private static MessageChannel mockMessageChannel(VoltronEntityReceivedResponse requestAckMessage, VoltronEntityAppliedResponse responseMessage) {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(requestAckMessage);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_APPLIED_RESPONSE)).thenReturn(responseMessage);
    return channel;
  }

  private ServerEntityRequest buildInvoke() {
    boolean requiresReplication = true;
    return new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.INVOKE_ACTION, payload, transactionID, TransactionID.NULL_ID, nodeID, requiresReplication, Optional.of(messageChannel));
  }
}
