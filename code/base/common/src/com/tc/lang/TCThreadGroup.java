/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

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

}
