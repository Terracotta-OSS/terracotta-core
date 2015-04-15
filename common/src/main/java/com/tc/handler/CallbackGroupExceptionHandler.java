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
package com.tc.handler;

import com.tc.lang.ServerExitStatus;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;

public class CallbackGroupExceptionHandler implements CallbackOnExitHandler {

  private final TCLogger logger;
  private final TCLogger consoleLogger;

  public CallbackGroupExceptionHandler(TCLogger logger, TCLogger consoleLogger) {
    this.logger = logger;
    this.consoleLogger = consoleLogger;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    logger.error(state.getThrowable().getMessage(), state.getThrowable());
    consoleLogger.error(state.getThrowable().getMessage());
    System.exit(ServerExitStatus.EXITCODE_STARTUP_ERROR);
  }
}
