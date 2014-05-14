/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.lang;

import com.tc.exception.ExceptionHelper;
import com.tc.logging.CallbackOnExitHandler;

public interface ThrowableHandler {

  void handleThrowable(Thread thread, Throwable throwable);

  void addHelper(ExceptionHelper helper);

  void handlePossibleOOME(Throwable t);

  void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler);

  void addCallbackOnExitExceptionHandler(Class c, CallbackOnExitHandler callbackOnExitHandler);

}
