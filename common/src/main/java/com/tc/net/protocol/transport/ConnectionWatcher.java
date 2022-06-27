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
package com.tc.net.protocol.transport;

import com.tc.net.protocol.tcm.ClientMessageChannel;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionWatcher implements MessageTransportListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionWatcher.class);
  private volatile ConnectionID connection;
  private final ClientConnectionEstablisher cce;
  private final ReferenceQueue<ClientMessageChannel> stopQueue = new ReferenceQueue<>();
  private volatile boolean stopped = false;
  private final WeakReference<ClientMessageChannel> targetHolder;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageChannel target, ClientConnectionEstablisher cce) {
    this.targetHolder = new WeakReference<>(target, stopQueue);
    this.cce = cce;
    this.connection = target.getConnectionID();
  }

  private boolean checkForStop() {
    Reference<? extends ClientMessageChannel> target = stopQueue.poll();
    if (target != null) {
      if (target == targetHolder) {
        stopped = true;
        LOGGER.warn("unreferenced connection left open {} {}", targetHolder.get(), connection);
        cce.shutdown();
      }
    }
    return stopped;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    cce.shutdown();
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportClosed(transport);
    }
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    if (transport.getProductID().isReconnectEnabled()) {
      cce.asyncReconnect(this::checkForStop);
    } else {
      cce.shutdown();
    }
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportDisconnected(transport, forcedDisconnect);
    }
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportConnectAttempt(transport);
    }
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    connection = transport.getConnectionID();
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportConnected(transport);
    }
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    cce.shutdown();
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportReconnectionRejected(transport);
    }
  }
}
