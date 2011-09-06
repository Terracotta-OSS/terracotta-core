/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * Helper for extracting proximate cause and ultimate cause from exceptions.
 */
public interface ExceptionHelper {

  /**
   * Check whether this helper accepts the kind of t
   * @param t Throwable
   * @return True if it accepts, false if not
   */
  public boolean accepts(Throwable t);
  
  /**
   * Get closest cause
   * @param t Throwable
   * @return Closest cause
   */
  public Throwable getProximateCause(Throwable t);

  /**
   * Get original cause
   * @param t Throwable
   * @return Original cause
   */
  public Throwable getUltimateCause(Throwable t);

}