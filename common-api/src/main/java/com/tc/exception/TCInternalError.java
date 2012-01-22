/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * An error type for serious (non-recoverable) error conditions in the Terracotta system. No production code should ever
 * catch this exception.
 */
public class TCInternalError extends TCError {

  public TCInternalError() {
    super("Terracotta Internal Error");
  }

  public TCInternalError(String message) {
    super(message);
  }

  public TCInternalError(Throwable cause) {
    super(cause);
  }

  public TCInternalError(String message, Throwable cause) {
    super(message, cause);
  }

}
