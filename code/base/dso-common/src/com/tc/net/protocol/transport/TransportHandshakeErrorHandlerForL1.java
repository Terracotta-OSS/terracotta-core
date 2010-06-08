/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

public class TransportHandshakeErrorHandlerForL1 implements TransportHandshakeErrorHandler {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  public void handleHandshakeError(TransportHandshakeErrorContext e) {
    // print error message on console
    if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) consoleLogger.error(e.getMessage());
    else consoleLogger.error(e);
    new Exception().printStackTrace();
    consoleLogger.error("I'm crashing the client!");
    try {
      Thread.sleep(30 * 1000);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    // at top layer DistributedObjectClient to kill this client
  }

}
