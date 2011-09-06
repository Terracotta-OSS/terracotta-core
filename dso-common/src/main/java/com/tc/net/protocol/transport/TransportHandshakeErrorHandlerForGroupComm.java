/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

public class TransportHandshakeErrorHandlerForGroupComm implements TransportHandshakeErrorHandler {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  public void handleHandshakeError(TransportHandshakeErrorContext e) {
    // print error message on console
    if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) consoleLogger.error(e.getMessage());
    else consoleLogger.error(e);
    // top layer at TCGroupMemberDiscoveryStatic to terminate connection
  }

}
