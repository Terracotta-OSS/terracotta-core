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

import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.RestoreConnectionCallback;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;

import java.util.TimerTask;

public class OOOReconnectionTimeout implements MessageTransportListener, RestoreConnectionCallback {

  private static final boolean                      debug            = false;

  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;
  private TimeoutTimerTask                          timeoutTimerTask = null;

  public OOOReconnectionTimeout(final OnceAndOnlyOnceProtocolNetworkLayer oooLayer, final long timeoutMillis) {
    this.oooLayer = oooLayer;
    this.timeoutMillis = timeoutMillis;
  }

  @Override
  public synchronized void notifyTransportClosed(MessageTransport transport) {
    log(transport, "Transport Closed");
    oooLayer.notifyTransportClosed(transport);
  }

  @Override
  public synchronized void notifyTransportConnectAttempt(MessageTransport transport) {
    oooLayer.notifyTransportConnectAttempt(transport);
  }

  @Override
  public synchronized void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    Assert.assertNull(this.timeoutTimerTask);
    if (oooLayer.isClosed()) { return; }
    if (forcedDisconnect) {
      log(transport, "Transport FORCE Disconnected, skipping opening reconnect window");
      oooLayer.connectionRestoreFailed();
    } else {
      log(transport, "Transport Disconnected, starting Timer for " + timeoutMillis);
      oooLayer.startRestoringConnection();
      oooLayer.notifyTransportDisconnected(transport, forcedDisconnect);
      // schedule timer task
      this.timeoutTimerTask = new TimeoutTimerTask(transport, this);
      oooLayer.getRestoreConnectTimer().schedule(this.timeoutTimerTask, timeoutMillis);
    }
  }

  @Override
  public synchronized void notifyTransportConnected(MessageTransport transport) {
    if (this.timeoutTimerTask != null) {
      log(transport, "Transport Connected, killing Timer for " + timeoutMillis);
      cancelTimerTask();
    }
    oooLayer.notifyTransportConnected(transport);
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    oooLayer.notifyTransportReconnectionRejected(transport);
  }

  private void cancelTimerTask() {
    this.timeoutTimerTask.cancel();
    this.timeoutTimerTask = null;
  }

  @Override
  public synchronized void restoreConnectionFailed(MessageTransport transport) {
    if (this.timeoutTimerTask != null) {
      log(transport, "Restore Connection Failed, killing Timer for " + timeoutMillis);
      oooLayer.connectionRestoreFailed();
      cancelTimerTask();
    }
  }

  static class TimeoutTimerTask extends TimerTask {
    private final MessageTransport          transport;
    private final RestoreConnectionCallback rcc;

    public TimeoutTimerTask(final MessageTransport transport, final RestoreConnectionCallback rcc) {
      super();
      this.transport = transport;
      this.rcc = rcc;
    }

    @Override
    public void run() {
      rcc.restoreConnectionFailed(transport);
    }
  }

  private static void log(MessageTransport transport, String msg) {
    if (debug) DebugUtil.trace("OOOTimer-SERVER-" + transport.getConnectionId() + " -> " + msg);
  }
}
