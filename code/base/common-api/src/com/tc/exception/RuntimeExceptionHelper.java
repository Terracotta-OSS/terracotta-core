/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

public class RuntimeExceptionHelper implements ExceptionHelper {

  public boolean accepts(Throwable t) {
    return t instanceof RuntimeException;
  }
  
  public Throwable getProximateCause(Throwable t) {
    return (t instanceof RuntimeException && ((RuntimeException) t).getCause() != null) ? ((RuntimeException) t)
        .getCause() : t;
  }

  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
