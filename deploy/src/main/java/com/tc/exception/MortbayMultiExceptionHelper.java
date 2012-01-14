/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

import org.mortbay.util.MultiException;

/**
 * Deal with Jetty MultiException to extract useful info
 */
public class MortbayMultiExceptionHelper extends AbstractExceptionHelper<MultiException> {

  public MortbayMultiExceptionHelper() {
    super(MultiException.class);
  }

  /**
   * Get closest cause, which is defined here as the first exception in a MultiException.
   * 
   * @param t MultiException
   * @return First in the MultiException
   */
  @Override
  public Throwable getProximateCause(Throwable t) {
    if (t instanceof MultiException) {
      MultiException m = (MultiException) t;
      if (m.size() > 0) return m.getThrowable(0);
    }
    return t;
  }

}
