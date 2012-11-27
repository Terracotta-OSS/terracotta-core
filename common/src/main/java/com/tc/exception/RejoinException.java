/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

public class RejoinException extends TCRuntimeException {

  public RejoinException() {
    super();
  }

  public RejoinException(String message) {
    super(message);
  }

  public RejoinException(String message, Throwable cause) {
    super(message, cause);
  }
}
