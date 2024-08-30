/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package com.tc.object.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;
import com.tc.object.msg.ClientHandshakeResponse;

public class ClientCoordinationHandler extends AbstractEventHandler<ClientHandshakeResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientCoordinationHandler.class);
  private final ClientHandshakeManager clientHandshakeManager;

  public ClientCoordinationHandler(ClientHandshakeManager clientHandshakeManager) {
    this.clientHandshakeManager = clientHandshakeManager;
  }

  @Override
  public void handleEvent(ClientHandshakeResponse context) {
    if (context instanceof ClientHandshakeRefusedMessage) {
      LOGGER.error(((ClientHandshakeRefusedMessage) context).getRefusalsCause());
      LOGGER.info("L1 Exiting...");
      throw new RuntimeException(((ClientHandshakeRefusedMessage) context).getRefusalsCause());
    } else if (context instanceof ClientHandshakeAckMessage) {
      handleClientHandshakeAckMessage((ClientHandshakeAckMessage) context);      
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handleClientHandshakeAckMessage(ClientHandshakeAckMessage handshakeAck) {
    clientHandshakeManager.acknowledgeHandshake(handshakeAck);
  }
}
