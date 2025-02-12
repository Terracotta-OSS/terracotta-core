/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
