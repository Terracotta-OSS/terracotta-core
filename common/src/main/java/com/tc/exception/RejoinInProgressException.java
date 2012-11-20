/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

public class RejoinInProgressException extends TCRuntimeException {
  public RejoinInProgressException() {
    this("Can not perform this operation because rejoin in progress");
  }

  public RejoinInProgressException(String message) {
    super(message);
  }

  public RejoinInProgressException(String message, Throwable cause) {
    super(message, cause);
  }
}
