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
package com.tc.lang;

import com.tc.exception.ExceptionHelper;
import com.tc.logging.CallbackOnExitHandler;

public interface ThrowableHandler {

  void handleThrowable(Thread thread, Throwable throwable);

  void addHelper(ExceptionHelper helper);

  void handlePossibleOOME(Throwable t);

  void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler);

  void addCallbackOnExitExceptionHandler(Class<?> c, CallbackOnExitHandler callbackOnExitHandler);

}
