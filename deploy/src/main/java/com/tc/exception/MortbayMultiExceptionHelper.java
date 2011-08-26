/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

import org.mortbay.util.MultiException;

/**
 * Deal with Jetty MultiException to extract useful info
 */
public class MortbayMultiExceptionHelper implements ExceptionHelper {

  /**
   * Accepts only the Jetty MultiException
   * @param t Throwable
   * @return True if Jetty MultiException
   */
  public boolean accepts(Throwable t) {
    return t instanceof MultiException;
  }

  /**
   * Get closest cause, which is defined here as the first exception
   * in a MultiException.
   * @param t MultiException
   * @return First in the MultiException
   */
  public Throwable getProximateCause(Throwable t) {
    if (t instanceof MultiException) {
      MultiException m = (MultiException) t;
      if (m.size() > 0) return m.getThrowable(0);
    }
    return t;
  }

  /**
   * No ultimate exception retrieved - always throws AssertionError
   * @param t Param ignored
   * @return Always AssertionError
   */
  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
