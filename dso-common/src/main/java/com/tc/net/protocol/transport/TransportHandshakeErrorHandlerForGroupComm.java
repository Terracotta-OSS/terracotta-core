/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.transport;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

public class TransportHandshakeErrorHandlerForGroupComm implements TransportHandshakeErrorHandler {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();

  @Override
  public void handleHandshakeError(TransportHandshakeErrorContext e) {
    // print error message on console
    if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) consoleLogger.error(e.getMessage());
    else consoleLogger.error(e);
    // top layer at TCGroupMemberDiscoveryStatic to terminate connection
  }

}
