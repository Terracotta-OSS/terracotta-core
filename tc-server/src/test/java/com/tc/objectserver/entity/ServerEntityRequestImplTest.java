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
package com.tc.objectserver.entity;

import org.junit.Before;
import org.junit.Test;

import com.tc.entity.VoltronEntityAppliedResponse;
import com.tc.entity.VoltronEntityReceivedResponse;
import com.tc.entity.VoltronEntityResponse;
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

  private void send(VoltronEntityResponse msg) {
    msg.send();
  }

  private static MessageChannel mockMessageChannel(VoltronEntityReceivedResponse requestAckMessage, VoltronEntityAppliedResponse responseMessage, VoltronEntityRetiredResponse retiredMessage) {
    MessageChannel channel = mock(MessageChannel.class);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_RECEIVED_RESPONSE)).thenReturn(requestAckMessage);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_COMPLETED_RESPONSE)).thenReturn(responseMessage);
    when(channel.createMessage(TCMessageType.VOLTRON_ENTITY_RETIRED_RESPONSE)).thenReturn(retiredMessage);
    return channel;
  }
}
