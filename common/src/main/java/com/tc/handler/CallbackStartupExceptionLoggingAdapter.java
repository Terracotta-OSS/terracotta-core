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
package com.tc.handler;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;

public class CallbackStartupExceptionLoggingAdapter implements CallbackOnExitHandler {

  private String extraMessage;

  public CallbackStartupExceptionLoggingAdapter() {
    this("");
  }

  public CallbackStartupExceptionLoggingAdapter(String extraMessage) {
    this.extraMessage = extraMessage;
  }

  @Override
  public void callbackOnExit(CallbackOnExitState state) {
    System.err.println("");
    System.err.println("");
    System.err.println("Fatal Terracotta startup exception:");
    System.err.println("");
    System.err.println(" " + state.getThrowable().getMessage() + extraMessage);
    System.err.println("");
    System.err.println("Server startup failed.");
  }
}
