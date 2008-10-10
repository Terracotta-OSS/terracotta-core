/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NullProtocolAdaptor;

public class TestConnectionHealthCheckerContextImpl extends ConnectionHealthCheckerContextImpl {

  private final TCConnectionManager  connectionManager;
  private final MessageTransportBase transport;
  private final HealthCheckerConfig  config;
  private final TCLogger             logger;

  public TestConnectionHealthCheckerContextImpl(MessageTransportBase mtb, HealthCheckerConfig config,
                                                TCConnectionManager connMgr) {
    super(mtb, config, connMgr);
    this.transport = mtb;
    this.config = config;
    this.logger = getLogger();
    this.connectionManager = connMgr;
  }

  protected TCConnection getNewConnection() {
    TCConnection connection = connectionManager.createConnection(new NullProtocolAdaptor());
    connection.addListener(this);
    return connection;
  }

  protected HealthCheckerSocketConnect getHealthCheckerSocketConnector(TCConnection presentConnection) {

    int callbackPort = transport.getRemoteCallbackPort();
    if (TransportHandshakeMessage.NO_CALLBACK_PORT == callbackPort) { return new NullHealthCheckerSocketConnectImpl(); }

    TCConnection conn = connectionManager.createConnection(new NullProtocolAdaptor());
    conn.addListener(this);

    TCSocketAddress sa = new TCSocketAddress(transport.getRemoteAddress().getAddress(), callbackPort);
    return new TestHealthCheckerSocketConnectImpl(sa, conn, transport.getRemoteAddress().getCanonicalStringForm(), logger,
                                              config.getSocketConnectTimeout());
  }

  public synchronized void closeEvent(TCConnectionEvent event) {
    //
  }

  public synchronized void connectEvent(TCConnectionEvent event) {
    // sorry, i am dumb. I cannot update the state mahcine.
  }

  public synchronized void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  public synchronized void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }
}
