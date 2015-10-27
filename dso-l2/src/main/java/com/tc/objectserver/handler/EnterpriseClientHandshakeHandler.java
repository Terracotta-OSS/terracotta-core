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
