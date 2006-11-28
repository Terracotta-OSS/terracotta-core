/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.exception;

public interface ExceptionHelper {

  public boolean accepts(Throwable t);
  
  public Throwable getProximateCause(Throwable t);

  public Throwable getUltimateCause(Throwable t);

}