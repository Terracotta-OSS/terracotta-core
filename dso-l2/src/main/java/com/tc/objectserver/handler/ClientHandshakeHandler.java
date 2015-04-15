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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.handshakemanager.ClientHandshakeException;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.handshakemanager.ServerClientModeInCompatibleException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ClientHandshakeHandler extends AbstractEventHandler {

  protected static final String        OPEN_SOURCE                = "Opensource";
  protected static final String        ENTERPRISE                 = "Enterprise";
  public static final String           LAGS                       = "lags";
  public static final String           LEADS                      = "leads";
  private static final int             SYSTEM_TIME_DIFF_THRESHOLD = TCPropertiesImpl
                                                                      .getProperties()
                                                                      .getInt(
                                                                              TCPropertiesConsts.TC_TIME_SYNC_THRESHOLD,
                                                                              30) * 1000;
  private static final TCLogger        LOGGER                     = TCLogging.getLogger(ClientHandshakeHandler.class);

  private ServerClientHandshakeManager handshakeManager;
  private final String                 serverName;

  public ClientHandshakeHandler(String serverName) {
    this.serverName = serverName;
  }

  @Override
  public void handleEvent(EventContext context) {
    final ClientHandshakeMessage clientMsg = ((ClientHandshakeMessage) context);
    try {
      NodeID remoteNodeID = clientMsg.getChannel().getRemoteNodeID();
      checkCompatibility(clientMsg.enterpriseClient(), remoteNodeID);
      checkTimeDifference(remoteNodeID, clientMsg.getLocalTimeMills());
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

  protected void checkCompatibility(boolean isEnterpriseClient, NodeID remoteNodeID)
      throws ServerClientModeInCompatibleException {
    if (isEnterpriseClient) {
      TerracottaOperatorEvent handshakeRefusedEvent = TerracottaOperatorEventFactory
          .createHandShakeRejectedEvent(ENTERPRISE, remoteNodeID, OPEN_SOURCE);
      logEvent(handshakeRefusedEvent);
      throw new ServerClientModeInCompatibleException("An " + ENTERPRISE + " client can not connect to an "
                                                      + OPEN_SOURCE + " Server, Connection refused.");
    }
  }

  protected void logEvent(TerracottaOperatorEvent event) {
    switch (event.getEventLevel()) {
      case INFO:
        LOGGER.info(event.getEventMessage());
        break;
      case WARN:
        LOGGER.warn(event.getEventMessage());
        break;
      case ERROR:
        LOGGER.error(event.getEventMessage());
        break;
      case DEBUG:
        LOGGER.debug(event.getEventMessage());
        break;
      case CRITICAL:
        LOGGER.fatal(event.getEventMessage());
        break;
      default:
        break;
    }
  }

  private void checkTimeDifference(NodeID remoteNodeID, long localTimeMills) {
    long timeDiff = System.currentTimeMillis() - localTimeMills;
    if (Math.abs(timeDiff) > SYSTEM_TIME_DIFF_THRESHOLD) {
      if (timeDiff > 0) {
        logEvent(TerracottaOperatorEventFactory.createSystemTimeDifferentEvent(remoteNodeID, LAGS, this.serverName,
                                                                               (Math.abs(timeDiff)) / 1000));
      } else {
        logEvent(TerracottaOperatorEventFactory.createSystemTimeDifferentEvent(remoteNodeID, LEADS, this.serverName,
                                                                               (Math.abs(timeDiff)) / 1000));
      }
    }
  }

  @Override
  public void initialize(ConfigurationContext ctxt) {
    super.initialize(ctxt);
    ServerConfigurationContext scc = ((ServerConfigurationContext) ctxt);
    this.handshakeManager = scc.getClientHandshakeManager();
  }

}
