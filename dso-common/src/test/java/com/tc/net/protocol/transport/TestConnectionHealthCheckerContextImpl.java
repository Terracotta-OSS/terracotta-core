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
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.protocol.NullProtocolAdaptor;

public class TestConnectionHealthCheckerContextImpl extends ConnectionHealthCheckerContextImpl {

  public TestConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                                TCConnectionManager connMgr) {
    super(mtb, config, connMgr);
  }

  @Override
  protected TCConnection getNewConnection(TCConnectionManager connectionManager) {
    TCConnection connection = connectionManager.createConnection(new NullProtocolAdaptor());
    return connection;
  }

  @Override
  protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection connection,
                                                                       MessageTransportBase transportBase,
                                                                       TCLogger loger, HealthCheckerConfig cnfg) {

    int callbackPort = transportBase.getRemoteCallbackPort();
    if (TransportHandshakeMessage.NO_CALLBACK_PORT == callbackPort) { return new NullHealthCheckerSocketConnectImpl(); }

    TCSocketAddress sa = new TCSocketAddress(transportBase.getRemoteAddress().getAddress(), callbackPort);
    return new TestHealthCheckerSocketConnectImpl(sa, connection, transportBase.getRemoteAddress()
        .getCanonicalStringForm()
                                                                  + "(callbackport:" + callbackPort + ")", loger, cnfg
        .getSocketConnectTimeout());
  }
}
