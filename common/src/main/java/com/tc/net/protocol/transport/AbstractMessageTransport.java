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

import org.slf4j.Logger;

import com.tc.logging.ConnectionIDProvider;
import com.tc.logging.ConnectionIdLogger;
import com.tc.net.protocol.NetworkLayer;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractMessageTransport implements MessageTransport, ConnectionIDProvider {

  private static final int           DISCONNECTED          = 1;
  private static final int           FORCED_DISCONNECT     = 2;
  private static final int           CONNECTED             = 3;
  private static final int           CONNECT_ATTEMPT       = 4;
  private static final int           CLOSED                = 5;
  private static final int           RECONNECTION_REJECTED = 6;

  protected ConnectionIdLogger       logger;
  private final CopyOnWriteArrayList<MessageTransportListener> listeners             = new CopyOnWriteArrayList<MessageTransportListener>();

  public AbstractMessageTransport(Logger logger) {
    this.logger = new ConnectionIdLogger(this, logger);
  }

  protected ConnectionIdLogger getLogger() {
    return logger;
  }

  @Override
  public final void addTransportListeners(List<MessageTransportListener> toAdd) {
    for (MessageTransportListener l : toAdd) {
      addTransportListener(l);
    }
  }

  /**
   * Returns an Unmodifiable view of the transport listeners.
   */
  protected List<MessageTransportListener> getTransportListeners() {
    return Collections.unmodifiableList(listeners);
  }

  @Override
  public void addTransportListener(MessageTransportListener listener) {
    if (!listeners.addIfAbsent(listener)) { throw new AssertionError(
                                                                     "Attempt to add the same listener more than once: "
                                                                         + listener); }
  }

  @Override
  public final void removeTransportListeners() {
    this.listeners.clear();
  }

  protected void fireTransportConnectAttemptEvent() {
    fireTransportEvent(CONNECT_ATTEMPT);
  }

  protected final void fireTransportConnectedEvent() {
    logFireTransportConnectEvent();
    fireTransportEvent(CONNECTED);
  }

  private void logFireTransportConnectEvent() {
    if (logger.isDebugEnabled()) {
      logger.debug("Firing connect event...");
    }
  }

  protected final void fireTransportForcedDisconnectEvent() {
    fireTransportEvent(FORCED_DISCONNECT);
  }

  protected final void fireTransportDisconnectedEvent() {
    fireTransportEvent(DISCONNECTED);
  }

  protected final void fireTransportClosedEvent() {
    fireTransportEvent(CLOSED);
  }

  protected final void fireTransportReconnectionRejectedEvent() {
    fireTransportEvent(RECONNECTION_REJECTED);
  }

  private void fireTransportEvent(int type) {
    for (MessageTransportListener listener : listeners) {
      switch (type) {
        case DISCONNECTED:
          listener.notifyTransportDisconnected(this, false);
          break;
        case FORCED_DISCONNECT:
          listener.notifyTransportDisconnected(this, true);
          break;
        case CONNECTED:
          listener.notifyTransportConnected(this);
          break;
        case CONNECT_ATTEMPT:
          listener.notifyTransportConnectAttempt(this);
          break;
        case CLOSED:
          listener.notifyTransportClosed(this);
          break;
        case RECONNECTION_REJECTED:
          listener.notifyTransportReconnectionRejected(this);
          break;
        default:
          throw new AssertionError("Unknown transport event: " + type);
      }
    }
  }

  @Override
  public short getCommunicationStackFlags(NetworkLayer parentLayer) {
    short stackLayerFlags = 0;
    while (parentLayer != null) {
      stackLayerFlags |= parentLayer.getStackLayerFlag();
      parentLayer = parentLayer.getReceiveLayer();
    }
    return stackLayerFlags;
  }

  @Override
  public String getCommunicationStackNames(NetworkLayer parentLayer) {
    StringBuilder currentLayer = new StringBuilder();
    while (parentLayer != null) {
      currentLayer.append("\n").append(parentLayer.getStackLayerName());
      parentLayer = parentLayer.getReceiveLayer();
    }
    return currentLayer.toString();
  }

  @Override
  public void initConnectionID(ConnectionID cid) {
    throw new UnsupportedOperationException();
  }
}
