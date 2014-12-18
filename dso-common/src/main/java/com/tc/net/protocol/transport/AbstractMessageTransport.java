/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
