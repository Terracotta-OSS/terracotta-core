/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

public class ExceptionHelper {
  public static Throwable getCauseOfType(Throwable t, Class causeType) {
    while (t != null) {
      if (causeType.isAssignableFrom(t.getClass())) { return t; }
      t = t.getCause();
    }
    return null;
  }

  public static Throwable getRootCause(Throwable t) {
    Throwable last = null;
    while (t != null) {
      last = t;
      t = t.getCause();
    }
    return last;
  }

}
