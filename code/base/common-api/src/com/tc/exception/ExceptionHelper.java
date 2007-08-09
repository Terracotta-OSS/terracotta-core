/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

public interface ExceptionHelper {

  public boolean accepts(Throwable t);
  
  public Throwable getProximateCause(Throwable t);

  public Throwable getUltimateCause(Throwable t);

}