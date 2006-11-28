/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

public class ShutdownError extends Error {

  public ShutdownError() {
    super();
  }

  public ShutdownError(String message) {
    super(message);
  }

  public ShutdownError(String message, Throwable cause) {
    super(message, cause);
  }

  public ShutdownError(Throwable cause) {
    super(cause);
  }

}
