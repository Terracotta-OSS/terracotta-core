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
package com.tc.net.protocol.transport;

import com.tc.logging.ConnectionIDProvider;
import com.tc.logging.ConnectionIdLogger;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.NetworkLayer;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractMessageTransport implements MessageTransport, ConnectionIDProvider {

  private enum TransportEvent {
    DISCONNECTED,
    FORCED_DISCONNECT,
    CONNECTED,
    CONNECT_ATTEMPT,
    CLOSED,
    RECONNECTION_REJECTED
  }

  protected ConnectionIdLogger       logger;
  private final CopyOnWriteArrayList listeners             = new CopyOnWriteArrayList();

  public AbstractMessageTransport(TCLogger logger) {
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
  protected List getTransportListeners() {
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
    fireTransportEvent(TransportEvent.CONNECT_ATTEMPT);
  }

  protected final void fireTransportConnectedEvent() {
    logFireTransportConnectEvent();
    fireTransportEvent(TransportEvent.CONNECTED);
  }

  private void logFireTransportConnectEvent() {
    if (logger.isDebugEnabled()) {
      logger.debug("Firing connect event...");
    }
  }

  protected final void fireTransportForcedDisconnectEvent() {
    fireTransportEvent(TransportEvent.FORCED_DISCONNECT);
  }

  protected final void fireTransportDisconnectedEvent() {
    fireTransportEvent(TransportEvent.DISCONNECTED);
  }

  protected final void fireTransportClosedEvent() {
    fireTransportEvent(TransportEvent.CLOSED);
  }

  protected final void fireTransportReconnectionRejectedEvent() {
    fireTransportEvent(TransportEvent.RECONNECTION_REJECTED);
  }

  private void fireTransportEvent(TransportEvent type) {
    for (Iterator i = listeners.iterator(); i.hasNext();) {
      MessageTransportListener listener = (MessageTransportListener) i.next();
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
