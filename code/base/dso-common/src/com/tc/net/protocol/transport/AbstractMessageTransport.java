/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.ConnectionIDProvider;
import com.tc.logging.ConnectionIdLogger;
import com.tc.logging.TCLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractMessageTransport implements MessageTransport, ConnectionIDProvider {

  private static final int           DISCONNECTED    = 1;
  private static final int           CONNECTED       = 2;
  private static final int           CONNECT_ATTEMPT = 3;
  private static final int           CLOSED          = 4;

  protected final ConnectionIdLogger logger;
  private final List                 listeners       = new LinkedList();

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

  protected List getTransportListeners() {
    return new ArrayList(listeners);
  }

  public void addTransportListener(MessageTransportListener listener) {
    synchronized (listeners) {
      if (listeners.contains(listener)) throw new AssertionError("Attempt to add the same listener more than once: "
                                                                 + listener);
      listeners.add(listener);
    }
  }

  public final void removeTransportListeners() {
    synchronized (listeners) {
      this.listeners.clear();
    }
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

  protected final void fireTransportDisconnectedEvent() {
    fireTransportEvent(DISCONNECTED);
  }

  protected final void fireTransportClosedEvent() {
    fireTransportEvent(CLOSED);
  }

  private void fireTransportEvent(int type) {
    synchronized (listeners) {
      for (Iterator i = listeners.iterator(); i.hasNext();) {
        MessageTransportListener listener = (MessageTransportListener) i.next();
        switch (type) {
          case DISCONNECTED:
            listener.notifyTransportDisconnected(this);
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
          default:
            throw new AssertionError("Unknown transport event: " + type);
        }
      }
    }
  }
}