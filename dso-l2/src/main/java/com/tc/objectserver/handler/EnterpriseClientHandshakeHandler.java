/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.net.NodeID;
import com.tc.objectserver.handshakemanager.ServerClientModeInCompatibleException;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class EnterpriseClientHandshakeHandler extends ClientHandshakeHandler {

  public EnterpriseClientHandshakeHandler(String serverName) {
    super(serverName);
  }

  private final TerracottaOperatorEventLogger opEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  @Override
  protected void checkCompatibility(boolean isEnterpriseClient, NodeID remoteNodeID)
      throws ServerClientModeInCompatibleException {
    if (!isEnterpriseClient) {
      TerracottaOperatorEvent handshakeRefusedEvent = TerracottaOperatorEventFactory
          .createHandShakeRejectedEvent(OPEN_SOURCE, remoteNodeID, ENTERPRISE);
      logEvent(handshakeRefusedEvent);
      throw new ServerClientModeInCompatibleException("An " + OPEN_SOURCE + " client can not connect to an "
                                                      + ENTERPRISE + " Server, Connection refused.");
    }
  }

  @Override
  protected void logEvent(TerracottaOperatorEvent event) {
    this.opEventLogger.fireOperatorEvent(event);
  }

}
