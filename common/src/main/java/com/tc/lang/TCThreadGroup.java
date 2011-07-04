/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.CallbackOnExitHandler;

public class TCThreadGroup extends ThreadGroup {

  private static final String    CLASS_NAME = TCThreadGroup.class.getName();

  private final ThrowableHandler throwableHandler;

  public static boolean currentThreadInTCThreadGroup() {
    return Thread.currentThread().getThreadGroup().getClass().getName().equals(CLASS_NAME);
  }

  public TCThreadGroup(ThrowableHandler throwableHandler) {
    this(throwableHandler, "TC Thread Group");
  }

  public TCThreadGroup(ThrowableHandler throwableHandler, String name) {
    super(name);
    this.throwableHandler = throwableHandler;
  }

  public void uncaughtException(Thread thread, Throwable throwable) {
    throwableHandler.handleThrowable(thread, throwable);
  }

  public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitDefaultHandler(callbackOnExitHandler);
  }
  
  public void addCallbackOnExitExceptionHandler(Class c, CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitExceptionHandler(c, callbackOnExitHandler);
  }
}
