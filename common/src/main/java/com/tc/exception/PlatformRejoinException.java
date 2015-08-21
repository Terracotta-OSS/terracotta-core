/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

public class PlatformRejoinException extends TCRuntimeException {
  public PlatformRejoinException() {
    this("can not perform operation because REJOIN-IN-PROGRESS");
  }

  public PlatformRejoinException(String message) {
    super(message);
  }

  public PlatformRejoinException(String message, Throwable cause) {
    super(message, cause);
  }
}
