/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ClientHandshakeException;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.handshakemanager.ServerClientModeInCompatibleException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;

public class ClientHandshakeHandler extends AbstractEventHandler {

  private static final String                 OPEN_SOURCE   = "Opensource";
  private static final String                 ENTERPRISE    = "Enterprise";
  private ServerClientHandshakeManager        handshakeManager;
  private final boolean                       isEnterpriseServer;
  private final TerracottaOperatorEventLogger opEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public ClientHandshakeHandler(boolean isEnterprise) {
    this.isEnterpriseServer = isEnterprise;
  }

  @Override
  public void handleEvent(EventContext context) {
    final ClientHandshakeMessage clientMsg = ((ClientHandshakeMessage) context);
    try {
      checkCompatibility(clientMsg.enterpriseClient());
      this.handshakeManager.notifyClientConnect(clientMsg);
    } catch (ClientHandshakeException e) {
      getLogger().error("Handshake Error : ", e);
      MessageChannel c = clientMsg.getChannel();
      getLogger().error("Closing channel " + c.getChannelID() + " because of previous errors");
      c.close();
    } catch (ServerClientModeInCompatibleException e) {
      getLogger().error("Handshake Error : ", e);
      this.handshakeManager.notifyClientRefused(clientMsg, e.getMessage());
      // client should go away after reading this message and the channel should get closed there by.
    }
  }

  private void checkCompatibility(boolean isEnterpriseClient) throws ServerClientModeInCompatibleException {
    if (this.isEnterpriseServer && !isEnterpriseClient) {
      TerracottaOperatorEvent handshakeRefusedEvent = TerracottaOperatorEventFactory
          .createHandShakeRejectedEvent(OPEN_SOURCE, ENTERPRISE);
      this.opEventLogger.fireOperatorEvent(handshakeRefusedEvent);
      throw new ServerClientModeInCompatibleException("An " + OPEN_SOURCE + " client can not connect to an "
                                                         + ENTERPRISE + " Server, Connection refused.");
    } else if (!this.isEnterpriseServer && isEnterpriseClient) {
      TerracottaOperatorEvent handshakeRefusedEvent = TerracottaOperatorEventFactory
          .createHandShakeRejectedEvent(ENTERPRISE, OPEN_SOURCE);
      this.opEventLogger.fireOperatorEvent(handshakeRefusedEvent);
      throw new ServerClientModeInCompatibleException("An " + ENTERPRISE + " client can not connect to an "
                                                         + OPEN_SOURCE + " Server, Connection refused.");
    }
  }

  @Override
  public void initialize(ConfigurationContext ctxt) {
    super.initialize(ctxt);
    ServerConfigurationContext scc = ((ServerConfigurationContext) ctxt);
    this.handshakeManager = scc.getClientHandshakeManager();
  }

}
