/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.context.PauseContext;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.msg.ClientHandshakeRefusedMessage;

public class ClientCoordinationHandler extends AbstractEventHandler {

  private static final TCLogger  consoleLogger = CustomerLogging.getConsoleLogger();
  private ClientHandshakeManager clientHandshakeManager;

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ClientHandshakeRefusedMessage) {
      consoleLogger.error(((ClientHandshakeRefusedMessage) context).getRefualsCause());
      consoleLogger.info("L1 Exiting...");
      throw new RuntimeException(((ClientHandshakeRefusedMessage) context).getRefualsCause());
    } else if (context instanceof ClientHandshakeAckMessage) {
      handleClientHandshakeAckMessage((ClientHandshakeAckMessage) context);
    } else if (context instanceof PauseContext) {
      handlePauseContext((PauseContext) context);
    } else {
      throw new AssertionError("unknown event type: " + context.getClass().getName());
    }
  }

  private void handlePauseContext(final PauseContext ctxt) {
    if (ctxt.getIsPause()) {
      clientHandshakeManager.disconnected(ctxt.getRemoteNode());
    } else {
      clientHandshakeManager.connected(ctxt.getRemoteNode());
    }
  }

  private void handleClientHandshakeAckMessage(final ClientHandshakeAckMessage handshakeAck) {
    clientHandshakeManager.acknowledgeHandshake(handshakeAck);
  }

  @Override
  public synchronized void initialize(final ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccContext = (ClientConfigurationContext) context;
    this.clientHandshakeManager = ccContext.getClientHandshakeManager();
  }

}
