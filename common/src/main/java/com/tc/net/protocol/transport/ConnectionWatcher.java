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

import com.tc.util.concurrent.SetOnceFlag;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionWatcher implements MessageTransportListener {

  private static Logger LOGGER = LoggerFactory.getLogger(ConnectionWatcher.class);
  private final ClientMessageTransport      cmt;
  private final ClientConnectionEstablisher cce;
  private final ReferenceQueue<MessageTransportListener> stopQueue = new ReferenceQueue<>();
  private final SetOnceFlag stopped = new SetOnceFlag();
  private final WeakReference<MessageTransportListener> targetHolder;

  /**
   * Listens to events from a MessageTransport, acts on them, and passes events through to target
   */
  public ConnectionWatcher(ClientMessageTransport cmt, MessageTransportListener target, ClientConnectionEstablisher cce) {
    this.cmt = cmt;
    this.targetHolder = new WeakReference<>(target, stopQueue);
    this.cce = cce;
  }
  
  private boolean checkForStop() {
    Reference<? extends MessageTransportListener> target = stopQueue.poll();
    if (target != null) {
      if (target == targetHolder) {
          stopped.set();
          LOGGER.warn("unreferenced connection left open");
      }
    }
    return stopped.isSet();
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    cce.quitReconnectAttempts();
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportClosed(transport);
    }
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    if (transport.getProductID().isReconnectEnabled()) {
      cce.asyncReconnect(cmt, this::checkForStop);
    } else {
      cce.quitReconnectAttempts();
      transport.close();
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
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportConnected(transport);
    }
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    cce.quitReconnectAttempts();
    MessageTransportListener target = targetHolder.get();
    if (target != null) {
      target.notifyTransportReconnectionRejected(transport);
    }
  }
}
