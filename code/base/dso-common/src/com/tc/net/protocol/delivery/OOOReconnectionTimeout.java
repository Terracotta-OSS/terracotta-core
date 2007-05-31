/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.transport.MessageTransport;
import com.tc.net.protocol.transport.MessageTransportListener;
import com.tc.net.protocol.transport.RestoreConnectionCallback;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.TimerTask;

public class OOOReconnectionTimeout implements MessageTransportListener, RestoreConnectionCallback {

  private final OnceAndOnlyOnceProtocolNetworkLayer oooLayer;
  private final long                                timeoutMillis;
  private TCTimerImpl                               timer = null;

  public OOOReconnectionTimeout(final OnceAndOnlyOnceProtocolNetworkLayer oooLayer, final long timeoutMillis) {
    this.oooLayer = oooLayer;
    this.timeoutMillis = timeoutMillis;
  }

  public synchronized void notifyTransportClosed(MessageTransport transport) {
    oooLayer.notifyTransportClosed(transport);
  }

  public synchronized void notifyTransportConnectAttempt(MessageTransport transport) {
    oooLayer.notifyTransportConnectAttempt(transport);
  }

  public synchronized void notifyTransportDisconnected(MessageTransport transport) {
    Assert.assertNull(timer);
    if (oooLayer.isClosed()) {
      return;
    }
    oooLayer.startRestoringConnection();
    oooLayer.notifyTransportDisconnected(transport);
    // start the timer...
    timer = new TCTimerImpl("ClientConnectionRestoreTimer", true);
    timer.schedule(new TimeoutTimerTask(transport, this), timeoutMillis);
  }

  public synchronized void notifyTransportConnected(MessageTransport transport) {
    if (timer != null) {
      cancelTimer();
    }
    oooLayer.notifyTransportConnected(transport);
  }

  private void cancelTimer() {
    timer.cancel();
    timer = null;
  }

  public synchronized void restoreConnectionFailed(MessageTransport transport) {
    if (timer != null) {
      oooLayer.connectionRestoreFailed();
      cancelTimer();
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

    public void run() {
      rcc.restoreConnectionFailed(transport);
    }
  }
}
