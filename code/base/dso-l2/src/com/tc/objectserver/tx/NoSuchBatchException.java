/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

public class NoSuchBatchException extends Exception {

  public NoSuchBatchException() {
    super();
  }

  public NoSuchBatchException(String message) {
    super(message);
  }

  public NoSuchBatchException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoSuchBatchException(Throwable cause) {
    super(cause);
  }

}
