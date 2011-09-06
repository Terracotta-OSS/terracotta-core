/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.concurrent.ThreadUtil;

public class TransportHandshakeErrorHandlerForL1 implements TransportHandshakeErrorHandler {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  public void handleHandshakeError(final TransportHandshakeErrorContext e) {
    if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) {
      consoleLogger.error(e.getMessage());
    } else {
      consoleLogger.error(e);
    }

    /**
     * These errors don't need sleep time before the next connect attempt. 1. ERROR_RECONNECTION_REJECTED: we want to
     * throw the rejected event asap so that rejoin will be attempted immediately. 2. ERROR_MAX_CONNECTION_EXCEED,
     * ERROR_STACK_MISMATCH : Client will be anyway killed at top layer by DOClient. However, Invalid ConnectionID and
     * other generic errors can be given some sleep time before the next connection attempt.
     */

    switch (e.getErrorType()) {
      case TransportHandshakeError.ERROR_STACK_MISMATCH:
      case TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED:
      case TransportHandshakeError.ERROR_RECONNECTION_REJECTED:
        // no sleep;
        break;
      default:
        ThreadUtil.reallySleep(30 * 1000);
    }

    switch (e.getErrorType()) {
      case TransportHandshakeError.ERROR_STACK_MISMATCH:
      case TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED:
        consoleLogger.error("Crashing the client due to handshake errors.");
        break;
    }

  }

}
