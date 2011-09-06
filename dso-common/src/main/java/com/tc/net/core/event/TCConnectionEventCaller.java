/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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
  private static final int  CONNECT      = 1;
  private static final int  EOF          = 2;
  private static final int  ERROR        = 3;
  private static final int  CLOSE        = 4;

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
      fireEvent(eventListeners, event, logger, ERROR);
    }
  }

  public void fireConnectEvent(List eventListeners, TCConnection conn) {
    if (connectEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CONNECT);
    }
  }

  public void fireEndOfFileEvent(List eventListeners, TCConnection conn) {
    if (eofEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, EOF);
    }
  }

  public void fireCloseEvent(List eventListeners, TCConnection conn) {
    if (closeEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CLOSE);
    }
  }

  private static void fireEvent(List eventListeners, TCConnectionEvent event, TCLogger logger, int type) {
    for (Iterator iter = eventListeners.iterator(); iter.hasNext();) {
      TCConnectionEventListener listener = (TCConnectionEventListener) iter.next();
      try {
        switch (type) {
          case CONNECT: {
            listener.connectEvent(event);
            break;
          }
          case EOF: {
            listener.endOfFileEvent(event);
            break;
          }
          case ERROR: {
            // cast is yucky here :-(
            listener.errorEvent((TCConnectionErrorEvent) event);
            break;
          }
          case CLOSE: {
            listener.closeEvent(event);
            break;
          }
          default: {
            throw new AssertionError("unknown event type: " + type);
          }
        }
      } catch (Exception e) {
        logger.error("Unhandled exception in event handler", e);
        throw new RuntimeException(e);
      }
    }
  }

}
