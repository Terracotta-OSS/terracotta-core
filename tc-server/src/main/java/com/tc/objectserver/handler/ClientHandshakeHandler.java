/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.objectserver.handler;


import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.l2.state.StateManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.api.EntityManager;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ClientHandshakeException;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.net.core.ProductID;
import com.tc.productinfo.ProductInfo;
import com.tc.productinfo.VersionCompatibility;
import com.tc.spi.Guardian;
import com.tc.util.version.Version;

public class ClientHandshakeHandler extends AbstractEventHandler<ClientHandshakeMessage> {

  private ServerClientHandshakeManager handshakeManager;
  private StateManager                 stateManager;
  private final EntityManager          entityManager;
  private final ProcessTransactionHandler transactionHandler;
  private final Version               serverVersion;
  private final VersionCompatibility versionCheck;

  public ClientHandshakeHandler(EntityManager entityManager, ProcessTransactionHandler transactionHandler, VersionCompatibility versionCheck) {
    this.entityManager = entityManager;
    this.transactionHandler = transactionHandler;
    this.serverVersion = new Version(ProductInfo.getInstance().version());
    this.versionCheck = versionCheck;
  }

  @Override
  public void handleEvent(ClientHandshakeMessage clientMsg) {
    String cid = clientMsg.getClientVersion() + ":" + clientMsg.getName() + ":" + clientMsg.getUUID() + ":" + clientMsg.getClientPID();
    String version = clientMsg.getClientVersion();
    Version client = new Version(version);

    try {
      if (clientMsg.isReconnect() && this.handshakeManager.isStarted()) {
        this.handshakeManager.notifyClientRefused(clientMsg, "server is not accepting reconnections");
      } else if (!GuardianContext.validate(Guardian.Op.CONNECT_CLIENT, cid, clientMsg.getChannel())) {
        this.handshakeManager.notifyClientRefused(clientMsg, "new connections not allowed");
      } else if (!versionCheck.isCompatibleClientServer(client.toString(), serverVersion.toString())) {
        this.handshakeManager.notifyClientRefused(clientMsg, "client version is not compatible than the server.  client version:" + client.toString() + " server version:" + serverVersion);
      } else if (clientMsg.getChannel().getProductID() == ProductID.DIAGNOSTIC) {
        this.handshakeManager.notifyDiagnosticClient(clientMsg);
      } else if (stateManager.isActiveCoordinator()) {
        this.handshakeManager.notifyClientConnect(clientMsg, entityManager, transactionHandler);
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
