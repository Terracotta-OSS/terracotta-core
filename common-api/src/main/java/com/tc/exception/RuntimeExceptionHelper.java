/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

/**
 * Exception helper for RuntimeException
 */
public class RuntimeExceptionHelper extends AbstractExceptionHelper<RuntimeException> {

  public RuntimeExceptionHelper() {
    super(RuntimeException.class);
  }
}
