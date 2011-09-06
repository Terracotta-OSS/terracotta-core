/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * Exception helper for RuntimeException
 */
public class RuntimeExceptionHelper implements ExceptionHelper {

  /**
   * Accepts RuntimeException
   * @param t Throwable
   * @return True if instanceof RuntimeException
   */
  public boolean accepts(Throwable t) {
    return t instanceof RuntimeException;
  }
  
  /**
   * Return chained exception
   * @param t RuntimeException
   * @return Cause of t
   */
  public Throwable getProximateCause(Throwable t) {
    return (t instanceof RuntimeException && ((RuntimeException) t).getCause() != null) ? ((RuntimeException) t)
        .getCause() : t;
  }

  /**
   * Always throws AssertiontError
   * @param t Param ignored
   * @return Always AssertionError
   */
  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
