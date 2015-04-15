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
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.transport.ClientConnectionEstablisher;
import com.tc.net.protocol.transport.ClientMessageTransport;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.RestoreConnectionCallback;
import com.tc.util.DebugUtil;

public class OOOConnectionWatcher implements RestoreConnectionCallback, MessageTransportListener {

  private static final boolean                      debug = Boolean.getBoolean("ooo.logging.enabled");

  protected final ClientMessageTransport            cmt;
  protected final ClientConnectionEstablisher       cce;
  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;

  public OOOConnectionWatcher(ClientMessageTransport cmt, ClientConnectionEstablisher cce,
                              OnceAndOnlyOnceProtocolNetworkLayer oooLayer, long timeoutMillis) {
    this.cmt = cmt;
    this.cce = cce;
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

  @Override
  public void restoreConnectionFailed(MessageTransport transport) {
    log(transport, "Restore Connection Failed");
    oooLayer.connectionRestoreFailed();

    // restore failed - try reconnect
    cce.asyncReconnect(cmt);
    // forcedDisconnect flag is not in above layer. So, defaulting to false
    oooLayer.notifyTransportDisconnected(transport, false);
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
    oooLayer.notifyTransportClosed(transport);
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    oooLayer.notifyTransportConnectAttempt(transport);
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    log(transport, "Reconnection Rejected");
    oooLayer.connectionRestoreFailed();
    oooLayer.notifyTransportReconnectionRejected(transport);
  }

  private static void log(MessageTransport transport, String msg) {
    if (debug) DebugUtil.trace("OOOConnectionWatcher-CLIENT-" + transport.getConnectionId() + " -> " + msg);
  }
}
