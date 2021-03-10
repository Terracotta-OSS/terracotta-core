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
package com.tc.server;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.server.ServerEnv;

public class CallbackShutdownExceptionLoggingAdapter implements CallbackOnExitHandler {
  Logger LOGGER = LoggerFactory.getLogger(CallbackShutdownExceptionLoggingAdapter.class);

  private String extraMessage;

  public CallbackShutdownExceptionLoggingAdapter() {
    this("");
  }

  public CallbackShutdownExceptionLoggingAdapter(String extraMessage) {
    this.extraMessage = extraMessage;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    if (ServerEnv.getServer().isStopped()) {
      LOGGER.debug("ignoring on shutdown", state.getThrowable());
    } else {
      throw new RuntimeException(state.getThrowable());
    }
  }
}
