/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.distrunner;

public class ArgException extends Exception {

  public ArgException() {
    super();
  }

  public ArgException(String message) {
    super(message);
  }

  public ArgException(String message, Throwable cause) {
    super(message, cause);
  }

  public ArgException(Throwable cause) {
    super(cause);
  }

}
