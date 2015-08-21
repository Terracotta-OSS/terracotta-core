/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

public class TCNotRunningException extends TCRuntimeException {

  public TCNotRunningException() {
    this("Terracotta is not running.");
  }

  public TCNotRunningException(String message, Throwable cause) {
    super(message, cause);
  }

  public TCNotRunningException(String message) {
    super(message);
  }

  public TCNotRunningException(Throwable cause) {
    super(cause);
  }

}
