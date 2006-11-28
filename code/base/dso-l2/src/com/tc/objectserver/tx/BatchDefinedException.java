/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

public class BatchDefinedException extends Exception {

  public BatchDefinedException() {
    super();
  }

  public BatchDefinedException(String message) {
    super(message);
  }

  public BatchDefinedException(String message, Throwable cause) {
    super(message, cause);
  }

  public BatchDefinedException(Throwable cause) {
    super(cause);
  }

}
