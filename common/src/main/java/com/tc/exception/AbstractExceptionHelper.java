/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  @Override
  public boolean accepts(Throwable t) {
    return tClass.isInstance(t);
  }

  /**
   * Return chained exception
   * 
   * @param t RuntimeException
   * @return Cause of t
   */
  @Override
  public Throwable getProximateCause(Throwable t) {
    return (accepts(t) && t.getCause() != null) ? t.getCause() : t;
  }

  /**
   * Always throws AssertiontError
   * 
   * @param t Param ignored
   * @return Always AssertionError
   */
  @Override
  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
