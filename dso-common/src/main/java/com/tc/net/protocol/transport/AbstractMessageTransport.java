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

  private static final int           DISCONNECTED          = 1;
  private static final int           FORCED_DISCONNECT     = 2;
  private static final int           CONNECTED             = 3;
  private static final int           CONNECT_ATTEMPT       = 4;
  private static final int           CLOSED                = 5;
  private static final int           RECONNECTION_REJECTED = 6;

  protected final ConnectionIdLogger logger;
  private final CopyOnWriteArrayList listeners             = new CopyOnWriteArrayList();

  public AbstractMessageTransport(TCLogger logger) {
    this.logger = new ConnectionIdLogger(this, logger);
  }

  public ConnectionIdLogger getLogger() {
    return logger;
  }

  public final void addTransportListeners(List toAdd) {
    for (Iterator i = toAdd.iterator(); i.hasNext();) {
      MessageTransportListener l = (MessageTransportListener) i.next();
      addTransportListener(l);
    }
  }

  /**
   * Returns an Unmodifiable view of the transport listeners.
   */
  protected List getTransportListeners() {
    return Collections.unmodifiableList(listeners);
  }

  public void addTransportListener(MessageTransportListener listener) {
    if (!listeners.addIfAbsent(listener)) { throw new AssertionError(
                                                                     "Attempt to add the same listener more than once: "
                                                                         + listener); }
  }

  public final void removeTransportListeners() {
    this.listeners.clear();
  }

  protected final void fireTransportConnectAttemptEvent() {
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
        default:
          throw new AssertionError("Unknown transport event: " + type);
      }
    }
  }

  public short getCommunicationStackFlags(NetworkLayer parentLayer) {
    short stackLayerFlags = 0;
    while (parentLayer != null) {
      stackLayerFlags |= parentLayer.getStackLayerFlag();
      parentLayer = parentLayer.getReceiveLayer();
    }
    return stackLayerFlags;
  }

  public String getCommunicationStackNames(NetworkLayer parentLayer) {
    StringBuilder currentLayer = new StringBuilder();
    while (parentLayer != null) {
      currentLayer.append("\n").append(parentLayer.getStackLayerName());
      parentLayer = parentLayer.getReceiveLayer();
    }
    return currentLayer.toString();
  }

  public void initConnectionID(ConnectionID cid) {
    throw new UnsupportedOperationException();
  }
}
