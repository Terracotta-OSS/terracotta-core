/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

/**
 * Base class for all exception helpers
 */
public abstract class AbstractExceptionHelper<V extends Throwable> implements ExceptionHelper {

  private final Class<V> tClass;

  public AbstractExceptionHelper(Class<V> handledClass) {
    tClass = handledClass;
  }

  /**
   * Test if given Throwable is accepted
   * 
   * @param t Throwable
   * @return True if t instanceof
   */
  public boolean accepts(Throwable t) {
    return tClass.isInstance(t);
  }

  /**
   * Return chained exception
   * 
   * @param t RuntimeException
   * @return Cause of t
   */
  public Throwable getProximateCause(Throwable t) {
    return (accepts(t) && t.getCause() != null) ? t.getCause() : t;
  }

  /**
   * Always throws AssertiontError
   * 
   * @param t Param ignored
   * @return Always AssertionError
   */
  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
