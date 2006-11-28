/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.exception;

import org.mortbay.util.MultiException;

public class MortbayMultiExceptionHelper implements ExceptionHelper {

  public boolean accepts(Throwable t) {
    return t instanceof MultiException;
  }

  public Throwable getProximateCause(Throwable t) {
    if (t instanceof MultiException) {
      MultiException m = (MultiException) t;
      if (m.size() > 0) return m.getThrowable(0);
    }
    return t;
  }

  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
