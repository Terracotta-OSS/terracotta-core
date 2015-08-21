/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
