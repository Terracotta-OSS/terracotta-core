/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
