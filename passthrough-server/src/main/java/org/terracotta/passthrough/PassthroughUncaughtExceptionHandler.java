/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.lang.Thread.UncaughtExceptionHandler;


/**
 * The uncaught exception handler installed for all the threads in the passthrough testing system.  All it does is log the
 * error and terminate the VM.
 */
public class PassthroughUncaughtExceptionHandler implements UncaughtExceptionHandler {
  public static final PassthroughUncaughtExceptionHandler sharedInstance = new PassthroughUncaughtExceptionHandler();

  @Override
  public void uncaughtException(Thread arg0, Throwable arg1) {
    System.err.println("FATAL EXCEPTION IN PASSTHROUGH THREAD:");
    arg1.printStackTrace();
  }
}
