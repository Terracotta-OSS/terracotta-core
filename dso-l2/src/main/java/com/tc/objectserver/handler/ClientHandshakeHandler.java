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
package com.tc.objectserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.l2.state.ConsistencyManager;
import com.tc.l2.state.StateManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ClientHandshakeException;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.util.ProductID;

public class ClientHandshakeHandler extends AbstractEventHandler<ClientHandshakeMessage> {

  private static final Logger LOGGER                     = LoggerFactory.getLogger(ClientHandshakeHandler.class);

  private ServerClientHandshakeManager handshakeManager;
  private StateManager                 stateManager;
  private final String                 serverName;
  private final EntityManager          entityManager;
  private final ProcessTransactionHandler transactionHandler;
  private final ConsistencyManager     consistencyMgr;

  public ClientHandshakeHandler(String serverName, EntityManager entityManager, ProcessTransactionHandler transactionHandler, ConsistencyManager cm) {
    this.serverName = serverName;
    this.entityManager = entityManager;
    this.transactionHandler = transactionHandler;
    this.consistencyMgr = cm;
  }

  @Override
  public void handleEvent(ClientHandshakeMessage clientMsg) {
    try {
      if (clientMsg.getChannel().getProductId() == ProductID.DIAGNOSTIC) {
        this.handshakeManager.notifyDiagnosticClient(clientMsg);
      } else if (stateManager.isActiveCoordinator()) {
        if (consistencyMgr.requestTransition(stateManager.getCurrentMode(), clientMsg.getSourceNodeID(), ConsistencyManager.Transition.ADD_CLIENT)) {
          this.handshakeManager.notifyClientConnect(clientMsg, entityManager, transactionHandler);
        } else {
          this.handshakeManager.notifyClientRefused(clientMsg, "new connections not allowed");
        }
      } else {
        this.handshakeManager.notifyClientRefused(clientMsg, "do not handshake with passive");
      }
    } catch (ClientHandshakeException e) {
      getLogger().error("Handshake Error : ", e);
      MessageChannel c = clientMsg.getChannel();
      getLogger().error("Closing channel " + c.getChannelID() + " because of previous errors");
      c.close();
    }
  }

  @Override
  public void initialize(ConfigurationContext ctxt) {
    super.initialize(ctxt);
    ServerConfigurationContext scc = ((ServerConfigurationContext) ctxt);
    this.handshakeManager = scc.getClientHandshakeManager();
    this.stateManager = scc.getL2Coordinator().getStateManager();
  }

}
