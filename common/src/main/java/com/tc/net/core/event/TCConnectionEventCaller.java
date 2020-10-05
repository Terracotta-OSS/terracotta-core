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
package com.tc.net.core.event;

import org.slf4j.Logger;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.SetOnceFlag;

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

  private final Logger logger;

  public TCConnectionEventCaller(Logger logger) {
    this.logger = logger;
  }

  public void fireErrorEvent(List<TCConnectionEventListener> eventListeners, TCConnection conn, Exception exception,
                             TCNetworkMessage context) {
    if (errorEvent.attemptSet()) {
      final TCConnectionErrorEvent event = new TCConnectionErrorEvent(conn, exception, context);
      fireEvent(eventListeners, event, logger, ERROR);
    }
  }

  public void fireConnectEvent(List<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (connectEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CONNECT);
    }
  }

  public void fireEndOfFileEvent(List<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (eofEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, EOF);
    }
  }

  public void fireCloseEvent(List<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (closeEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CLOSE);
    }
  }

  private static void fireEvent(List<TCConnectionEventListener> eventListeners, TCConnectionEvent event, Logger logger, int type) {
    for (TCConnectionEventListener listener : eventListeners) {
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
