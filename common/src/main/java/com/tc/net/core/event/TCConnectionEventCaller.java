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
package com.tc.net.core.event;

import org.slf4j.Logger;

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.Collection;

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

  public void fireErrorEvent(Collection<TCConnectionEventListener> eventListeners, TCConnection conn, Exception exception,
                             TCNetworkMessage context) {
    if (errorEvent.attemptSet()) {
      final TCConnectionErrorEvent event = new TCConnectionErrorEvent(conn, exception, context);
      fireEvent(eventListeners, event, logger, ERROR);
    }
  }

  public void fireConnectEvent(Collection<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (connectEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CONNECT);
    }
  }

  public void fireEndOfFileEvent(Collection<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (eofEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, EOF);
    }
  }

  public void fireCloseEvent(Collection<TCConnectionEventListener> eventListeners, TCConnection conn) {
    if (closeEvent.attemptSet()) {
      final TCConnectionEvent event = new TCConnectionEvent(conn);
      fireEvent(eventListeners, event, logger, CLOSE);
    }
  }

  private static void fireEvent(Collection<TCConnectionEventListener> eventListeners, TCConnectionEvent event, Logger logger, int type) {
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
