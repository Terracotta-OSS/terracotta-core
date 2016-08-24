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

package com.tc.object.servermessage;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventHandlerException;
import com.tc.entity.ServerEntityMessage;
import com.tc.entity.ServerEntityResponseMessage;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.ClientEntityManager;
import com.tc.object.EntityDescriptor;
import com.tc.util.Assert;


public class ServerMessageReceiveHandler<EC> extends AbstractEventHandler<EC> {
  private ClientEntityManager clientEntityManager;
  private final ClientMessageChannel clientMessageChannel;

  public ServerMessageReceiveHandler(ClientMessageChannel clientMessageChannel) {
    this.clientMessageChannel = clientMessageChannel;
  }

  @Override
  public void handleEvent(EC context) throws EventHandlerException {
    ServerEntityMessage message = (ServerEntityMessage) context;
    EntityDescriptor entityDescriptor = message.getEntityDescriptor();
    clientEntityManager.handleMessage(entityDescriptor, message.getMessage());
    Long responseId = message.getResponseId();
    if (responseId != null) {
      ServerEntityResponseMessage response = (ServerEntityResponseMessage) clientMessageChannel.createMessage(TCMessageType.SERVER_ENTITY_RESPONSE_MESSAGE);
      response.setResponseId(responseId);
      Assert.assertTrue(response.send());
    };
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    ClientConfigurationContext configurationContext = (ClientConfigurationContext) context;
    this.clientEntityManager = configurationContext.getEntityManager();
  }
}
