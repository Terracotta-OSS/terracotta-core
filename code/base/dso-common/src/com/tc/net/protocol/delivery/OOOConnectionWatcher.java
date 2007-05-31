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

public class OOOConnectionWatcher extends ConnectionWatcher implements RestoreConnectionCallback {

  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;

  public OOOConnectionWatcher(ClientMessageTransport cmt, ClientConnectionEstablisher cce,
                              OnceAndOnlyOnceProtocolNetworkLayer oooLayer, long timeoutMillis) {
    super(cmt, oooLayer, cce);
    this.oooLayer = oooLayer;
    this.timeoutMillis = timeoutMillis;
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    oooLayer.startRestoringConnection();
    oooLayer.notifyTransportDisconnected(transport);
    cce.asyncRestoreConnection(cmt, transport.getRemoteAddress(), this, timeoutMillis);
  }

  public void notifyTransportConnected(MessageTransport transport) {
    oooLayer.notifyTransportConnected(transport);
  }

  public void restoreConnectionFailed(MessageTransport transport) {
    oooLayer.connectionRestoreFailed();
    super.notifyTransportDisconnected(transport);
  }
}
