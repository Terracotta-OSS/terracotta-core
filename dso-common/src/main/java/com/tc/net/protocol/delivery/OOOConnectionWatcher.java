/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.ConnectionWatcher;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.RestoreConnectionCallback;
import com.tc.util.DebugUtil;

public class OOOConnectionWatcher extends ConnectionWatcher implements RestoreConnectionCallback {

  private static final boolean                      debug = false;

  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;

  public OOOConnectionWatcher(ClientMessageTransport cmt, ClientConnectionEstablisher cce,
                              OnceAndOnlyOnceProtocolNetworkLayer oooLayer, long timeoutMillis) {
    super(cmt, oooLayer, cce);
    this.oooLayer = oooLayer;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    oooLayer.startRestoringConnection();
    oooLayer.notifyTransportDisconnected(transport, forcedDisconnect);
    if (!forcedDisconnect) {
      log(transport, "Transport Disconnected, calling asyncRestoreConnection for " + timeoutMillis);
      cce.asyncRestoreConnection(cmt, transport.getRemoteAddress(), this, timeoutMillis);
    } else {
      log(transport, "Transport FORCE Disconnect. Skipping asyncRestoreConnection.");
      restoreConnectionFailed(transport);
    }
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    log(transport, "Transport Connected");
    oooLayer.notifyTransportConnected(transport);
  }

  public void restoreConnectionFailed(MessageTransport transport) {
    log(transport, "Restore Connection Failed");
    oooLayer.connectionRestoreFailed();
    // forcedDisconnect flag is not in above layer. So, defaultingh to false
    super.notifyTransportDisconnected(transport, false);
  }

  private static void log(MessageTransport transport, String msg) {
    if (debug) DebugUtil.trace("OOOConnectionWatcher-CLIENT-" + transport.getConnectionId() + " -> " + msg);
  }
}
