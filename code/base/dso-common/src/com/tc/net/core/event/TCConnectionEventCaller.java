/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core.event;

import com.tc.logging.TCLogger;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Iterator;
import java.util.List;

// calls each event only once
public class TCConnectionEventCaller {
  private final SetOnceFlag connectEvent = new SetOnceFlag();
  private final SetOnceFlag eofEvent     = new SetOnceFlag();
  private final SetOnceFlag errorEvent   = new SetOnceFlag();
  private final SetOnceFlag closeEvent   = new SetOnceFlag();
  private final TCLogger    logger;

  public TCConnectionEventCaller(TCLogger logger) {
    this.logger = logger;
  }

  public void fireErrorEvent(List eventListeners, TCConnection conn, final Exception exception,
                             final TCNetworkMessage context) {
    if (errorEvent.attemptSet()) {
      final TCConnectionErrorEvent event = new TCConnectionErrorEvent(conn, exception, context);
      fireErrorEvent(eventListeners, event, logger);
    }
  }

  public static void fireErrorEvent(List eventListeners, final TCConnectionErrorEvent event, final TCLogger logger) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
      try {
        listener.errorEvent(event);
      } catch (Exception e) {
        logger.error("Unhandled exception in event handler", e);
      }
    }
  }

  public void fireConnectEvent(List eventListeners, TCConnection conn) {
    if (connectEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireConnectEvent(eventListeners, event, logger);
    }
  }

  public static void fireConnectEvent(List eventListeners, final TCConnectionEvent event, final TCLogger logger) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
      try {
        listener.connectEvent(event);
      } catch (Exception e) {
        logger.error("Unhandled exception in event handler", e);
      }
    }
  }

  public void fireEndOfFileEvent(List eventListeners, TCConnection conn) {
    if (eofEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEndOfFileEvent(eventListeners, event, logger);
    }
  }

  public static void fireEndOfFileEvent(List eventListeners, final TCConnectionEvent event, final TCLogger logger) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
      try {
        listener.endOfFileEvent(event);
      } catch (Exception e) {
        logger.error("Unhandled exception in event handler", e);
      }
    }
  }

  public void fireCloseEvent(List eventListeners, TCConnection conn) {
    if (closeEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireCloseEvent(eventListeners, event, logger);
    }
  }

  public static void fireCloseEvent(List eventListeners, final TCConnectionEvent event, TCLogger logger) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
      try {
        listener.closeEvent(event);
      } catch (Exception e) {
        logger.error("Unhandled exception in event handler", e);
      }
    }
  }

}
