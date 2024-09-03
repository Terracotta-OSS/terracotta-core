/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.util.concurrent.SetOnceFlag;
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
  private final SetOnceFlag stopped = new SetOnceFlag();
  private final WeakReference<ClientMessageChannel> targetHolder;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageChannel target, ClientConnectionEstablisher cce) {
  // this the channel is no longer reachable, make sure all the the connection threads are cleaned up
    this.targetHolder = new WeakReference<>(target, stopQueue);
    this.cce = cce;
    this.connection = target.getConnectionID();
  }

  private boolean checkForStop() {
    Reference<? extends ClientMessageChannel> target = stopQueue.poll();
    while (target != null) {
      if (target == targetHolder) {
        stopped.set();
        LOGGER.warn("unreferenced connection left open {} {}", targetHolder.get(), connection);
        cce.shutdown();
      }
      target = stopQueue.poll();
    }
    return stopped.isSet();
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
