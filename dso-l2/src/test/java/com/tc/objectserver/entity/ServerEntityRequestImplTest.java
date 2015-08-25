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
    ServerEntityRequest serverEntityRequest = new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.CREATE_ENTITY, payload, transactionID, nodeID, requiresReplication, Optional.of(messageChannel));

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
    return new ServerEntityRequestImpl(entityDescriptor, ServerEntityAction.INVOKE_ACTION, payload, transactionID, nodeID, requiresReplication, Optional.of(messageChannel));
  }
}
