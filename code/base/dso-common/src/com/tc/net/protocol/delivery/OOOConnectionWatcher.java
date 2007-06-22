/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionWatcher;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.RestoreConnectionCallback;
import com.tc.util.DebugUtil;

public class OOOConnectionWatcher extends ConnectionWatcher implements RestoreConnectionCallback {

  private static final boolean                      debug = true;

  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;

  public OOOConnectionWatcher(ClientMessageTransport cmt, ClientConnectionEstablisher cce,
                              OnceAndOnlyOnceProtocolNetworkLayer oooLayer, long timeoutMillis) {
    super(cmt, oooLayer, cce);
    this.oooLayer = oooLayer;
    this.timeoutMillis = timeoutMillis;
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    log(transport, "Transport Disconnected, calling asyncRestoreConnection for " + timeoutMillis);
    oooLayer.startRestoringConnection();
    oooLayer.notifyTransportDisconnected(transport);
    cce.asyncRestoreConnection(cmt, transport.getRemoteAddress(), this, timeoutMillis);
  }

  public void notifyTransportConnected(MessageTransport transport) {
    log(transport, "Transport Connected");
    oooLayer.notifyTransportConnected(transport);
  }

  public void restoreConnectionFailed(MessageTransport transport) {
    log(transport, "Restore Connection Failed");
    oooLayer.connectionRestoreFailed();
    super.notifyTransportDisconnected(transport);
  }

  private static void log(MessageTransport transport, String msg) {
    if (debug) DebugUtil.trace("OOOConnectionWatcher-CLIENT-" + transport.getConnectionId() + " -> " + msg);
  }
}
